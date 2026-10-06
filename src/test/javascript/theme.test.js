const fs = require('fs');
const { loadApp, readIndexHtml, APP_PATH } = require('./setup/loadApp');

const CSS_PATH = APP_PATH.replace(/app\.js$/, 'style.css');

function click(document, id) {
  document.getElementById(id).dispatchEvent(new window.Event('click', { bubbles: true }));
}

function theme() {
  return document.documentElement.getAttribute('data-theme');
}

/**
 * Runs the inline <head> script from index.html, the way the browser does before the
 * first paint. loadApp() only mounts <body>, so this script is exercised on its own.
 */
function runHeadScript() {
  const head = readIndexHtml().match(/<head>([\s\S]*)<\/head>/)[1];
  const code = head.match(/<script>([\s\S]*?)<\/script>/)[1];
  new Function(code)();
}

/** The data-theme the page declares in its markup, before any script runs. */
function declaredTheme() {
  return readIndexHtml().match(/<html[^>]*\sdata-theme="([^"]+)"/)[1];
}

describe('theme toggle (TODO-231)', () => {
  beforeEach(() => {
    window.localStorage.clear();
    document.documentElement.setAttribute('data-theme', declaredTheme());
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  test('AC-4: the page is dark by default, with nothing stored', async () => {
    expect(declaredTheme()).toBe('dark');
    runHeadScript();
    expect(theme()).toBe('dark');
    await loadApp();
    expect(theme()).toBe('dark');
  });

  test('AC-4: the OS colour-scheme setting is ignored', () => {
    const sources = [readIndexHtml(), fs.readFileSync(CSS_PATH, 'utf8'), fs.readFileSync(APP_PATH, 'utf8')];
    for (const source of sources) {
      expect(source).not.toMatch(/prefers-color-scheme|matchMedia/);
    }
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

    document.documentElement.setAttribute('data-theme', declaredTheme());
    await loadApp();
    expect(theme()).toBe('light');
    expect(document.getElementById('theme-toggle').textContent).toBe('Dark theme');
  });

  test('AC-3: a stored theme is applied before first paint, without waiting for app.js', () => {
    window.localStorage.setItem('ops-theme', 'light');
    runHeadScript();
    expect(theme()).toBe('light');
  });

  test('a junk stored value or blocked storage falls back to dark', async () => {
    window.localStorage.setItem('ops-theme', 'purple');
    runHeadScript();
    expect(theme()).toBe('dark');
    await loadApp();
    expect(theme()).toBe('dark');

    jest.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('storage blocked');
    });
    jest.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('storage blocked');
    });
    runHeadScript();
    expect(theme()).toBe('dark');
    const { document } = await loadApp();
    expect(theme()).toBe('dark');
    click(document, 'theme-toggle');
    expect(theme()).toBe('light');
  });
});
