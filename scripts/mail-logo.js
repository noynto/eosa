// Génère le logo des e-mails (src/main/resources/public/mail/logo.png) : « eosa. » en Bagel Fat One, couleur `primary`,
// sur le fond de la carte des e-mails (`bg`), en 224 × 72 px pour un affichage en 112 × 36 px (écrans Retina).
// Commande et explications : docs/tech/emails.md (« Regénérer le logo »).
const fs = require('fs');
const puppeteer = require('puppeteer');

const [fontFile, outFile] = process.argv.slice(2);

(async () => {
    // La police est incorporée en base64 : une page sans adresse ne peut pas charger un fichier local.
    const font = fs.readFileSync(fontFile).toString('base64');
    const browser = await puppeteer.launch({headless: true, args: ['--no-sandbox']});
    const page = await browser.newPage();
    await page.setViewport({width: 112, height: 36, deviceScaleFactor: 2});
    await page.setContent(`<style>
        @font-face { font-family: 'Bagel Fat One'; src: url(data:font/woff2;base64,${font}) format('woff2'); }
        html, body { margin: 0; background: #FDFBF7; }
        div { width: 112px; height: 36px; display: flex; align-items: center;
              font-family: 'Bagel Fat One'; font-size: 32px; line-height: 1; color: #601414; }
    </style><div>eosa.</div>`);
    await page.evaluate(() => document.fonts.load("32px 'Bagel Fat One'"));
    await page.evaluate(() => document.fonts.ready);
    await page.screenshot({path: outFile, clip: {x: 0, y: 0, width: 112, height: 36}});
    await browser.close();
})();
