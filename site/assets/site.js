const reduced=window.matchMedia('(prefers-reduced-motion: reduce)');
if (!reduced.matches && 'IntersectionObserver' in window) {
  const parts=document.querySelectorAll('.section-heading, .feature, .showcase-copy, .shot, .story-image, .story-copy, .faq-section > div, .close');
  const observer=new IntersectionObserver(entries=>entries.forEach(entry=>{if(entry.isIntersecting){entry.target.classList.add('revealed');observer.unobserve(entry.target)}}),{threshold:.08});
  parts.forEach(part=>{part.classList.add('reveal');observer.observe(part)});
}
