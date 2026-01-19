import gguf
import numpy as np
import torch
import torch.nn as nn
import torchvision.datasets as dsets
import torchvision.transforms as transforms
import sys
from time import time

# Hyperparameters
num_epochs = 15
batch_size = 64
lr = 1e-3


class MnistCNN(nn.Module):
    """
    CNN architecture matching SKaiNET's createMNISTCNN():
    - Conv1: 1→16 channels, 5x5 kernel, padding 2, stride 1 → ReLU → MaxPool 2x2 → 14x14x16
    - Conv2: 16→32 channels, 5x5 kernel, padding 2, stride 1 → ReLU → MaxPool 2x2 → 7x7x32
    - Flatten: 7*7*32 = 1568
    - Dense: 1568→10
    """
    def __init__(self):
        super(MnistCNN, self).__init__()

        # Conv block 1: 1→16 channels
        # Named to match SKaiNET: "stage1.conv1"
        self.stage1 = nn.Sequential()
        self.stage1.conv1 = nn.Conv2d(
            in_channels=1,
            out_channels=16,
            kernel_size=5,
            stride=1,
            padding=2
        )

        # Conv block 2: 16→32 channels
        # Named to match SKaiNET: "stage2.conv2"
        self.stage2 = nn.Sequential()
        self.stage2.conv2 = nn.Conv2d(
            in_channels=16,
            out_channels=32,
            kernel_size=5,
            stride=1,
            padding=2
        )

        # Output layer: 1568→10
        # Named to match SKaiNET: "out"
        self.out = nn.Linear(7 * 7 * 32, 10)

        self.relu = nn.ReLU()
        self.pool = nn.MaxPool2d(kernel_size=2, stride=2)

    def forward(self, x):
        # Conv block 1
        x = self.stage1.conv1(x)
        x = self.relu(x)
        x = self.pool(x)  # 28x28 → 14x14

        # Conv block 2
        x = self.stage2.conv2(x)
        x = self.relu(x)
        x = self.pool(x)  # 14x14 → 7x7

        # Flatten and output
        x = x.view(x.size(0), -1)  # Flatten to (batch, 1568)
        x = self.out(x)
        return x


def train(model_path):
    # Load MNIST dataset
    train_data = dsets.MNIST(
        root='./data',
        train=True,
        transform=transforms.ToTensor(),
        download=True
    )
    test_data = dsets.MNIST(
        root='./data',
        train=False,
        transform=transforms.ToTensor()
    )

    print(f"Training samples: {len(train_data)}")
    print(f"Test samples: {len(test_data)}")

    train_loader = torch.utils.data.DataLoader(
        dataset=train_data,
        batch_size=batch_size,
        shuffle=True,
        num_workers=4,
        pin_memory=True
    )
    test_loader = torch.utils.data.DataLoader(
        dataset=test_data,
        batch_size=batch_size,
        shuffle=False,
        num_workers=4,
        pin_memory=True
    )

    # Create model
    model = MnistCNN()

    # Print model architecture
    print("\nModel architecture:")
    print(model)
    print("\nModel parameters:")
    for name, param in model.named_parameters():
        print(f"  {name}: {list(param.shape)}")

    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    print(f"\nUsing device: {device}")
    model = model.to(device)

    loss_fn = nn.CrossEntropyLoss()
    optimizer = torch.optim.Adam(model.parameters(), lr=lr)

    # Training loop
    print("\nTraining...")
    t_start = time()

    for epoch in range(num_epochs):
        model.train()
        running_loss = 0.0
        correct = 0
        total = 0

        for i, (images, labels) in enumerate(train_loader):
            images = images.to(device)
            labels = labels.to(device)

            optimizer.zero_grad()
            outputs = model(images)
            loss = loss_fn(outputs, labels)
            loss.backward()
            optimizer.step()

            running_loss += loss.item()
            _, predicted = torch.max(outputs.data, 1)
            total += labels.size(0)
            correct += (predicted == labels).sum().item()

        epoch_loss = running_loss / len(train_loader)
        epoch_acc = 100 * correct / total
        print(f"Epoch [{epoch+1:2d}/{num_epochs}] Loss: {epoch_loss:.4f}, Train Acc: {epoch_acc:.2f}%")

    print(f"\nTraining completed in {time() - t_start:.2f}s")

    # Evaluation
    model.eval()
    correct = 0
    total = 0

    with torch.no_grad():
        for images, labels in test_loader:
            images = images.to(device)
            labels = labels.to(device)
            outputs = model(images)
            _, predicted = torch.max(outputs.data, 1)
            total += labels.size(0)
            correct += (predicted == labels).sum().item()

    test_acc = 100 * correct / total
    print(f"\nTest Accuracy: {test_acc:.2f}%")

    # Export to GGUF
    print(f"\nExporting to GGUF: {model_path}")
    model = model.cpu()

    gguf_writer = gguf.GGUFWriter(model_path, "mnist-cnn")

    # Map PyTorch state dict names to SKaiNET expected names
    name_mapping = {
        "stage1.conv1.weight": "stage1.conv1.weight",
        "stage1.conv1.bias": "stage1.conv1.bias",
        "stage2.conv2.weight": "stage2.conv2.weight",
        "stage2.conv2.bias": "stage2.conv2.bias",
        "out.weight": "out.weight",
        "out.bias": "out.bias",
    }

    state_dict = model.state_dict()
    print("\nTensors saved to GGUF:")

    for pytorch_name, gguf_name in name_mapping.items():
        if pytorch_name in state_dict:
            data = state_dict[pytorch_name].numpy()
            print(f"  {gguf_name}: {list(data.shape)}")
            gguf_writer.add_tensor(gguf_name, data)
        else:
            print(f"  WARNING: {pytorch_name} not found in state dict!")

    gguf_writer.write_header_to_file()
    gguf_writer.write_kv_data_to_file()
    gguf_writer.write_tensors_to_file()
    gguf_writer.close()

    print(f"\nModel saved to {model_path}")


if __name__ == '__main__':
    if len(sys.argv) != 2:
        print(f"Usage: {sys.argv[0]} <output_model_path.gguf>")
        print(f"Example: {sys.argv[0]} mnist_cnn.gguf")
        sys.exit(1)

    train(sys.argv[1])
