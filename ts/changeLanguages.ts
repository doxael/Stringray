/*****************************************************************************
Copyright (c) 2008-2020 - Maxprograms,  http://www.maxprograms.com/

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to compile,
modify and use the Software in its executable form without restrictions.

Redistribution of this Software or parts of it in any form (source code or
executable binaries) requires prior written permission from Maxprograms.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*****************************************************************************/

class ChangeLanguages {

    electron = require('electron');

    constructor() {
        this.electron.ipcRenderer.send('get-theme');
        this.electron.ipcRenderer.send('get-languages');
        this.electron.ipcRenderer.on('set-languages', (event, arg) => {
            this.setLanguages(arg);
        });
        this.electron.ipcRenderer.on('set-theme', (event, arg) => {
            (document.getElementById('theme') as HTMLLinkElement).href = arg;
        });
        document.addEventListener('keydown', (event) => {
            if (event.key === 'Escape') {
                window.close();
            }
            if (event.key === 'Enter') {
                this.saveLanguages();
            }
        });
        document.getElementById('save').addEventListener('click', () => {
            this.saveLanguages();
        });
        this.electron.ipcRenderer.on('get-height', () => {
            let body: HTMLBodyElement = document.getElementById('body') as HTMLBodyElement;
            this.electron.ipcRenderer.send('languages-height', { width: body.clientWidth, height: body.clientHeight });
        });
        this.electron.ipcRenderer.on('language-pair', (event, arg) => {
            this.setFileLanguages(arg);
        });
    }

    setLanguages(arg: any): void {
        var array = arg.languages;
        var languageOptions = '';
        for (let i = 0; i < array.length; i++) {
            var lang = array[i];
            languageOptions = languageOptions + '<option value="' + lang.code + '">' + lang.description + '</option>';
        }
        document.getElementById('srcLangSelect').innerHTML = languageOptions;
        document.getElementById('tgtLangSelect').innerHTML = languageOptions;
        this.electron.ipcRenderer.send('file-languages');
    }

    setFileLanguages(arg: any) {
        (document.getElementById('srcLangSelect') as HTMLSelectElement).value = arg.srcLang;
        (document.getElementById('tgtLangSelect') as HTMLSelectElement).value = arg.tgtLang;
    }

    saveLanguages() {
        var prefs: any = {
            srcLang: (document.getElementById('srcLangSelect') as HTMLSelectElement).value,
            tgtLang: (document.getElementById('tgtLangSelect') as HTMLSelectElement).value,
        }
        this.electron.ipcRenderer.send('save-languages', prefs);
    }
}

new ChangeLanguages();




