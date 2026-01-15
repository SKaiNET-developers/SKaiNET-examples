// This script adds emoji support to the web application
document.addEventListener('DOMContentLoaded', function() {
    // Function to apply the emoji class to elements containing emoji characters
    function applyEmojiClass() {
        // List of emoji characters used in the app
        const emojiList = ['✓', '🖼️', '⬇️', '🗑️'];
        
        // Find all text nodes in the document
        const walker = document.createTreeWalker(
            document.body,
            NodeFilter.SHOW_TEXT,
            null,
            false
        );
        
        const textNodes = [];
        let node;
        while (node = walker.nextNode()) {
            textNodes.push(node);
        }
        
        // Check each text node for emoji characters
        textNodes.forEach(textNode => {
            let hasEmoji = false;
            for (const emoji of emojiList) {
                if (textNode.nodeValue.includes(emoji)) {
                    hasEmoji = true;
                    break;
                }
            }
            
            // If the text node contains an emoji, wrap it in a span with the emoji class
            if (hasEmoji && textNode.parentNode) {
                const span = document.createElement('span');
                span.className = 'emoji';
                span.textContent = textNode.nodeValue;
                textNode.parentNode.replaceChild(span, textNode);
            }
        });
    }
    
    // Apply emoji class initially
    applyEmojiClass();
    
    // Set up a MutationObserver to apply emoji class to new elements
    const observer = new MutationObserver(function(mutations) {
        applyEmojiClass();
    });
    
    // Start observing the document with the configured parameters
    observer.observe(document.body, { 
        childList: true, 
        subtree: true 
    });
});