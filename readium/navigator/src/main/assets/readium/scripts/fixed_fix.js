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
            margin: 0;
            padding: 0;
            width: 100%;
            height: 100%;
            overflow: hidden;
            background-color: white;
        }
        .main {
            width: 100%;
            height: 100%;
            display: flex;
            align-items: center;
            justify-content: center;
            background-color: white;
          }
         svg {
            max-width: 100%;
            max-height: 100%;
            object-fit: contain;
            display: block;
        }
        image {
            width: 100%;
            height: 100%;
             display: block;
            object-fit: contain;
        }
    `;
    document.head.appendChild(style);
})();