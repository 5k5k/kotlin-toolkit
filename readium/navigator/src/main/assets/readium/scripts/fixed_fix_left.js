(function retry(maxAttempts = 10, attempt = 1) {
      const svg = document.querySelector('svg');
    if (svg) {
        svg.setAttribute('preserveAspectRatio', 'xMinYMid meet');
        return
    }
    if (attempt < maxAttempts) {
        setTimeout(() => retry(maxAttempts, attempt + 1), 1);
    }
})();