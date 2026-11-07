function openToggleDown1(element) {
    const allDetails = element.closest('details.box').querySelectorAll('details');
    const isRestore = Array.from(allDetails.values()).some(e => e.hasAttribute('data-open'));
    const set = isRestore ? 'open' : 'data-open';
    const remove = isRestore ? 'data-open' : 'open';
    allDetails.forEach(details => {
        if (details.hasAttribute(remove)) {
            details.setAttribute(set, '');
            details.removeAttribute(remove);
        }
    });
}

function openRecursiveUp(element) {
    while (element != null && element.tagName.toLowerCase() === 'details') {
        element.setAttribute('open', '');
        element = element.parentElement.closest('details');
    }
}

function setLocationPathnameFile(name) {
    const path = window.location.pathname;
    const base = path.substring(0, path.lastIndexOf('/'))
    window.location.pathname = base + '/'+name;
}

function setLocationSearch(name, value, blank) {
    const searchParams = new URLSearchParams(window.location.search)
    if (value === '' || value == null) {
        searchParams.delete(name);
    } else {
        searchParams.set(name, value);
    }
    setLocationSearchParams(searchParams, blank);
}

function removeLocationSearch(name, value) {
    const searchParams = new URLSearchParams(window.location.search)
    searchParams.delete(name, value);
    setLocationSearchParams(searchParams);
}

function modifyLocationSearch(button) {
    const select = button.parentElement.querySelector('select');
    const value = select.value;
    if (value === '') return;
    const key = select.getAttribute("data-key");
    const params = new URLSearchParams(window.location.search);
    const scope = key + ':' + value
    if (button.value === '+') {
        if (!params.has('scope', scope)) params.append('scope', scope);
    } else {
        params.set('scope', scope);
    }
    window.location.hash = '';
    setLocationSearchParams(params);
}

function setLocationSearchParams(params, blank) {
    // undo : and / escaping for URL readability
    const search = params.toString().replaceAll('%3A', ':').replaceAll('%2F', '/');
    if (blank) {
        let currentUrl = window.location.href;
        let updatedUrl = new URL(currentUrl);
        updatedUrl.search = search;
        window.open(updatedUrl.href, '_blank');
    } else {
        window.location.search = search;
    }
}

function addHashHotkey() {
    const id = 'hk_'+location.hash.substring(1);
    if (document.getElementById(id) != null) return;
    const a = document.createElement('a');
    const hotkeys = document.getElementById("hotkeys");
    hotkeys.appendChild(a);
    const fn = document.createElement('kbd');
    let n = hotkeys.childNodes.length-1;
    if (n > 9) {
        hotkeys.firstChild.nextSibling.remove();
        n = 1;
    }
    fn.appendChild(document.createTextNode("Ctrl+"+ n));
    fn.id = 'hk'+n;
    a.appendChild(fn);
    a.appendChild(document.createTextNode(" "+location.hash+" "));
    a.href = location.hash;
    a.id = id;
    a.title = location.hash;
}

function schemaUsages(details) {
    if (details.getElementsByTagName('div').length > 0) {
        return;
    }
    // fill...
    const id = details.parentNode.closest('details').id;
    const links = document.querySelectorAll('section a[href="#'+id+'"]');
    const box = document.createElement('div');
    details.appendChild(box);
    const targets = Array.from(links).map(node => node.closest('[id]'));
    const uniqueTargets = targets.filter((t, index) => targets.indexOf(t) === index);
    uniqueTargets.forEach(target => {
        if (target.id !== id) {
            const a = document.createElement('a');
            a.appendChild(document.createTextNode(target.id));
            a.href = '#'+target.id;
            box.appendChild(a);
        }
    });
}

window.addEventListener('hashchange', (e) => {
    openRecursiveUp(document.getElementById(location.hash.substring(1)));
    addHashHotkey();
}, false);

document.addEventListener("DOMContentLoaded", (e) => {
    if (!location.hash) return;
    openRecursiveUp(document.getElementById(location.hash.substring(1)));
});

window.addEventListener('keydown', function(e) {
    if (e.ctrlKey && e.key.match(/[0-9]/)) {
        var t = document.getElementById("hk"+e.key);
        if (t != null)  {
            t.parentElement.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true, view: window }));
            e.stopPropagation();
            e.preventDefault();
        }
    }
});