# Ekrany a makiety — odchylenia

Porównanie: `cd stepday-web && npm run e2e` (Playwright, 390 px, `docker compose up` musi działać).
Wzorce to strony PDF z makietami (`docs/Stepday · widok dnia.pdf`, kolejność plansz z `canvas.json`),
zrzuty aplikacji i zestawienia: `docs/screenshots/{mockup,app,compare}/<ekran>.png`.

Ekrany dnia, edytorów i układu działają na prawdziwym API z danymi z makiet (`e2e/seed.ts`).
Podsumowanie tygodnia dostaje odpowiedź API z fixture'a (`e2e/fixtures.ts`) — te same dane backend
wylicza w `WeekSummaryTest` (tydzień 2–8.11 odegrany przez API).

Wynik: układ, typografia, kolory, odstępy i ikony zgodne 1:1 (po wyrównaniu `box-sizing` do makiet).

## Odchylenia wynikające z danych lub decyzji

| Ekran | Makieta | Aplikacja | Powód |
|---|---|---|---|
| Main | „pole 10 z 30” | „pole 2 z 30” | Dane: w seedzie zaliczony jest tylko 9.11 (D1: pole = zaliczone dni miesiąca + 1). Przy 9 zaliczonych dniach byłoby „pole 10”. |
| Main | — | Dwie ikony 44 px w wierszu „Stepday” (Układ dnia, Podsumowanie tygodnia) | Makiety nie pokazują nawigacji między ekranami. Ikony z zestawu makiet, wiersz nie zmienia wysokości. |
| Main | — | Karta „Koniec dnia” widoczna pod ścieżką | Jest w makiecie, ale poza wysokością planszy (1140 px). |
| Main | Przycisk „Filtr wymieniony” | „Zrobione” | Model danych nie ma etykiety akcji per blok; tekst z makiety jest specyficzny dla tego bloku. |
| Main | „Bez kary, seria zostaje” tylko przy liście | Tylko przy liście | Zgodnie z makietą. |
| Main | — | Chip „Zaległe · przeniesione N×” na bloku przeniesionym (CARRY_OVER) | Stan nieobecny w makietach; styl chipa „Z kalendarza”. |
| Licznik | „Przypomnienia: Co 2 godziny, 8:00–20:00” | „Przypomnienie: 07:30” (jedna godzina) | D7: model ma jedno `reminder_time`. |
| Licznik, Lista | Godzina „7:30” jako tekst | Natywne pole czasu („07:30” + ikona zegara w Chrome) | Natywny picker na iPhonie; na iOS bez ikony. |
| Licznik | „−” przy „Jedno kliknięcie” aktywny | Nieaktywny przy wartości 1 | Krok nie może spaść poniżej 1. |
| Typ | Szablony aktywne | Nieaktywne („wkrótce”) | D8: treść szablonów nieokreślona. |
| Kiedy | — | Dla „Wybrane dni tygodnia” przyciski Pn–Nd, dla „Raz w miesiącu” wybór dnia | Makieta rozwija tylko „Co jakiś czas”; pozostałe opcje w tym samym stylu. |
| Braki | Piątek: Woda, potem Spacer | Spacer, potem Woda | Kolejność z zapytania dokumentu (`ORDER BY bi.sort_key`); makieta jest w tym niespójna (środa ma Spacer przed Wodą). |
| Braki | „6 z 7 dni zaliczone” | to samo; dla 1: „dzień zaliczony” | Odmiana. |
| Sukces, Braki | — | Wiersz „Tydzień jeszcze trwa”, gdy tydzień nie jest skończony | Stan nieobecny w makietach. |
| Wszystkie | — | Ekran logowania | A2: tryb jednego użytkownika, brak modułu logowania. |
| Main (iPhone) | — | Baner „Dodaj Stepday do ekranu początkowego” | Wymóg: Web Push tylko w zainstalowanej PWA. Pokazywany na iOS poza trybem standalone, do zamknięcia. |
