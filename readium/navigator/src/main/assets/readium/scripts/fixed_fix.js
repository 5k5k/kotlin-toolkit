(function() {
    var viewport = document.querySelector('meta[name="viewport"]');
    if (viewport) {
        viewport.setAttribute('content', 'width=device-width, initial-scale=1.0');
    } else {
        var newViewport = document.createElement('meta');
        newViewport.name = 'viewport';
        newViewport.content = 'width=device-width, initial-scale=1.0';
        document.head.appendChild(newViewport);
    }

    var style = document.createElement('style');
    style.innerHTML = `
        html, body {
            margin: 0 !important;
            padding: 0 !important;
            width: 100% !important;
            height: 100% !important;
            overflow: hidden !important;
        }

        svg {
            display: block !important;
            width: 100vw !important;
            height: 100vh !important;
            object-fit: contain !important;
        }

        svg[viewBox] {
            preserveAspectRatio: xMidYMid meet !important;
        }

        svg image {
            width: 100% !important;
            height: 100% !important;
            object-fit: contain !important;
            preserveAspectRatio: xMidYMid meet !important;
        }

        div.main, div {
            display: flex !important;
            justify-content: center !important;
            align-items: center !important;
            width: 100% !important;
            height: 100% !important;
        }
    `;
    document.head.appendChild(style);
})();