# Instagram Token Tracker (Java Swing)

Desktop aplikacija u Javi 17 za praćenje i poređenje Instagram followers/following lista,
sa bezbednim OAuth prijavljivanjem i lokalnim čuvanjem podataka.

## Funkcionalnosti
- **Analiza lista**: ko te ne prati nazad, koga ti ne pratiš nazad, mutuals, ko te je otpratio, novi pratioci, statistika.
- **Uvoz iz fajla**: Instagram *Download your information* export (`followers_1.json`, `following.json`, HTML) ili obični `.txt` / `.csv`.
- **Snapshot-ovi** u lokalnoj SQLite bazi (čuvanje, učitavanje, brisanje) za poređenje u različitim vremenskim tačkama.
- **Izvoz rezultata u CSV**.
- **Instagram OAuth 2.0 login** kroz browser (lozinka se nikad ne unosi u aplikaciju):
  - lokalni callback server na `127.0.0.1`, `state` parametar zaštićen od CSRF-a,
  - timeout od 3 minuta i garantovano gašenje servera (port se ne zaključava),
  - access token se čuva **enkriptovan AES-256-GCM** (nasumičan IV po poruci) u SQLite bazi.
- AES ključ se **automatski generiše** pri prvom pokretanju.
- Zatraženi OAuth scope je samo `instagram_business_basic` (princip najmanjih privilegija).

## Pokretanje
Potrebni su Java 17+ i Maven.

```bash
mvn test          # unit testovi
mvn compile exec:java
```

Aplikacija radi odmah, bez ikakvog podešavanja, za uvoz i analizu lista.
Podaci su u `~/.ig-token-tracker/` (`tracker.db`, `config.json`).

## Instagram prijava (opciono)
Prijava zahteva Meta for Developers aplikaciju i **profesionalni** (business/creator) nalog.
1. Napravi Instagram app na Meta for Developers i dodaj redirect URI `http://127.0.0.1:8085/callback`.
2. U aplikaciji: *Podesi Meta app podatke* → unesi Client ID i Client Secret.

**Ograničenje:** zvanični Instagram API za profesionalne naloge ne vraća liste followers/following,
pa se prijava koristi za autentifikaciju i čuvanje tokena, a same liste se unose uvozom fajla ili ručno.

## Struktura
- `auth/` – OAuth login i lokalni callback server
- `crypto/` – AES-GCM enkripcija
- `db/` – SQLite inicijalizacija
- `service/` – repozitorijumi, config, poređenje lista, uvoz Instagram exporta
- `ui/` – Swing glavni prozor
- `src/test/` – JUnit 5 testovi (poređenje, uvoz, enkripcija, baza, config, OAuth callback)

## Tehnologije
Java 17, Swing + FlatLaf, SQLite (JDBC), Gson, JUnit 5, Maven.
