# Deegwekker

Android-app om brood en pizzadeeg te bakken zonder telkens zelf een wekker te zetten.
Eén tik op **Start rijzing** plant alle timers van een recept in één keer: stretch & folds,
einde bulk, vormen, oven voorverwarmen en uit de oven halen.

## Wat zit erin

- **Home** met de lopende bak (aftelling, voortgang, eindtijd), tegels voor Brood en Pizzadeeg, en "Opnieuw bakken".
- **Recepten**: drie broodsjablonen (desem zelfde dag, desem koelkastnacht, snel gistbrood) en twee pizzadegen. Elk recept kopiëren en aanpassen.
- **Recept nakijken**: wachttijden per stap aanpassen met + / –, live "Klaar om …", en Start.
- **Lopende bak**: grote aftelling, tijdlijn met kloktijden, Gedaan / +5 min / Pauzeer, stap verschuiven.
- **Alarmen** op het vergrendelscherm met Gedaan en Nog 5 min, een vaste melding met aftelling, en een vooraankondiging 5 minuten vóór vormen of bakken.
- **Pizzadeeg-calculator** voor Napolitaans en Romeins (tonda), met bakkerspercentages en verse of droge gist.

## Twee soorten stappen

- **Op de klok**: het alarm gaat op een vast moment af (stretch & fold, deksel eraf). De planning schuift niet als je het alarm later afzet.
- **Wacht op mij**: handwerk zoals kneden of vormen. De volgende wachttijd telt pas vanaf het moment dat je op Gedaan tikt; tot dan herinnert de app je elke 5 minuten.

De planning wordt bewaard als absolute tijden. Na een herstart van de telefoon worden de alarmen opnieuw gezet.

## Installeren

Elke push bouwt een APK in GitHub Actions. Open de laatste run onder **Actions → Android**,
download het artefact `deegwekker-apk`, pak het zip-bestand uit en open `app-debug.apk` op je telefoon
(sta "installeren uit onbekende bronnen" toe voor je browser of bestandsbeheer).

Bij de eerste start vraagt de app om meldingen, exacte alarmen en volledig-scherm-meldingen.

## Bouwen

```
./gradlew testDebugUnitTest assembleDebug
```

Kotlin + Jetpack Compose, minSdk 26, targetSdk 35. De logica (planning, alarmen, calculator) zit in
`app/src/main/java/.../domain` en heeft unit tests in `app/src/test`.
