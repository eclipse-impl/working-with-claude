const { loadApp } = require('./setup/loadApp');

function click(document, id) {
  document.getElementById(id).dispatchEvent(new window.Event('click', { bubbles: true }));
}

function theme() {
  return document.documentElement.getAttribute('data-theme');
}

describe('theme toggle (TODO-231)', () => {
  beforeEach(() => {
    window.localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
    delete window.matchMedia;
  });

  test('AC-4: dark by default when nothing is stored, even if the OS prefers light', async () => {
    window.matchMedia = jest.fn((query) => ({
      matches: query === '(prefers-color-scheme: light)',
      media: query,
      addEventListener() {},
      removeEventListener() {}
    }));
    await loadApp();
    expect(theme()).toBe('dark');
  });

  test('AC-1, AC-2: clicking the toggle switches data-theme between dark and light', async () => {
    const { document } = await loadApp();
    click(document, 'theme-toggle');
    expect(theme()).toBe('light');
    click(document, 'theme-toggle');
    expect(theme()).toBe('dark');
  });

  test('AC-1: the toggle label names the theme you will get', async () => {
    const { document } = await loadApp();
    const button = document.getElementById('theme-toggle');
    expect(button.textContent).toBe('Light theme');
    click(document, 'theme-toggle');
    expect(button.textContent).toBe('Dark theme');
  });

  test('AC-3: the choice is stored and restored on the next load', async () => {
    const first = await loadApp();
    click(first.document, 'theme-toggle');
    expect(window.localStorage.getItem('ops-theme')).toBe('light');

    document.documentElement.removeAttribute('data-theme');
    await loadApp();
    expect(theme()).toBe('light');
    expect(document.getElementById('theme-toggle').textContent).toBe('Dark theme');
  });
});
