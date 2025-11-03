(function retry(maxAttempts = 10, attempt = 1) {
      const svg = document.querySelector('svg');
    if (svg) {
        svg.setAttribute('preserveAspectRatio', 'xMinYMid meet');
    }
     const img = document.querySelector('img');
     if (img) {
         img.style.objectFit = 'contain';
         img.style.objectPosition = 'left center';
         img.style.width = '100%';
         img.style.height = 'auto';
     }
     if (svg || img) return;

    if (attempt < maxAttempts) {
        setTimeout(() => retry(maxAttempts, attempt + 1), 1);
    }
})();