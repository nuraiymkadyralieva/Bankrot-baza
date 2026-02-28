package auction;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class BankrotParser {

    private static final long SLEEP_BETWEEN_LOTS_MS = 1200;
    private static final int HUMAN_CHECK_TIMEOUT_SEC = 90;

    // сколько максимум ждать, пока ты войдёшь/зарегистрируешься (для документов)
    private static final int LOGIN_TIMEOUT_SEC = 120;

    public List<LotData> parseLots(String baseUrl, int maxLotsToParse) {

        ChromeOptions options = new ChromeOptions();
        // options.addArguments("--headless=new"); // не включаем, чтобы можно было пройти проверку руками

        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
        driver.manage().window().maximize();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(25));
        List<LotData> lots = new ArrayList<>();

        try {
            int currentPage = 1;

            while (lots.size() < maxLotsToParse) {
                String pageUrl = (currentPage == 1) ? baseUrl : (baseUrl + "&page=" + currentPage);

                System.out.println("\n=== PAGE " + currentPage + " ===");
                System.out.println("URL: " + pageUrl);

                driver.get(pageUrl);

                // 1) дождаться body
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("body")));

                // 2) если есть антибот — ждём/даём пройти руками
                waitForHumanCheckIfNeeded(driver, pageUrl);

                // 3) НА ПЕРВОЙ СТРАНИЦЕ даём авторизоваться (чтобы появились документы)
                if (currentPage == 1) {
                    waitForLoginIfNeeded(driver);

                    // после логина перезагрузим страницу списка — иногда контент обновляется только после reload
                    driver.get(pageUrl);
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("body")));
                }

                // 4) ищем ссылки на лоты
                List<WebElement> lotLinksElements = findLotLinks(driver);

                if (lotLinksElements.isEmpty()) {
                    System.out.println("Лоты не найдены даже после проверки/логина.");
                    System.out.println("ACTUAL URL: " + driver.getCurrentUrl());
                    System.out.println("TITLE: " + driver.getTitle());
                    break;
                }

                List<String> lotUrlsOnPage = new ArrayList<>();
                for (WebElement el : lotLinksElements) {
                    String href = el.getAttribute("href");
                    if (href != null && !href.isBlank()) lotUrlsOnPage.add(href);
                }

                for (String url : lotUrlsOnPage) {
                    if (lots.size() >= maxLotsToParse) break;

                    System.out.println("\nПарсинг (" + (lots.size() + 1) + "/" + maxLotsToParse + "): " + url);

                    driver.get(url);
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("body")));

                    LotData lot = parseLotPage(driver);

                    printTz10(lot); // ✅ уже без даты окончания
                    lots.add(lot);

                    try { Thread.sleep(SLEEP_BETWEEN_LOTS_MS); } catch (InterruptedException ignored) {}
                }

                currentPage++;
            }

        } finally {
            driver.quit();
        }

        return lots;
    }

    // =========================
    // АНТИБОТ / HUMAN CHECK
    // =========================
    private static void waitForHumanCheckIfNeeded(WebDriver driver, String pageUrl) {
        long start = System.currentTimeMillis();

        while (true) {
            String title = safe(driver.getTitle());
            String url = safe(driver.getCurrentUrl());

            System.out.println("ACTUAL URL: " + url);
            System.out.println("TITLE: " + title);

            boolean isHumanCheck =
                    title.toLowerCase(Locale.ROOT).contains("проверка пользователя")
                            || title.toLowerCase(Locale.ROOT).contains("checking")
                            || url.toLowerCase(Locale.ROOT).contains("captcha");

            if (!isHumanCheck) return;

            long elapsedSec = (System.currentTimeMillis() - start) / 1000;
            if (elapsedSec > HUMAN_CHECK_TIMEOUT_SEC) {
                System.out.println("Проверка не исчезла за " + HUMAN_CHECK_TIMEOUT_SEC + " сек. Останавливаемся.");
                return;
            }

            System.out.println("⚠️ Вижу антибот/проверку. Пройди её в браузере (осталось ~" +
                    (HUMAN_CHECK_TIMEOUT_SEC - elapsedSec) + " сек).");

            try { Thread.sleep(1500); } catch (InterruptedException ignored) {}

            driver.get(pageUrl);
        }
    }

    // =========================
    // АВТОРИЗАЦИЯ (ВРУЧНУЮ)
    // =========================
    private static void waitForLoginIfNeeded(WebDriver driver) {
        long start = System.currentTimeMillis();

        System.out.println("\n🔐 Если документы открываются только после входа — сейчас войди/зарегистрируйся.");
        System.out.println("   Я подожду до " + LOGIN_TIMEOUT_SEC + " секунд и потом продолжу парсинг.\n");

        while (true) {
            // пытаемся понять, что пользователь уже вошёл
            String src = "";
            try {
                src = driver.getPageSource().toLowerCase(Locale.ROOT);
            } catch (Exception ignore) {}

            boolean looksLoggedOut =
                    src.contains("войти") || src.contains("регистрац")
                            || src.contains("login") || src.contains("sign in");

            boolean looksLoggedIn =
                    src.contains("выйти") || src.contains("logout")
                            || src.contains("профиль") || src.contains("личный кабинет");

            if (looksLoggedIn && !looksLoggedOut) {
                System.out.println("✅ Похоже, ты авторизована. Продолжаем.");
                return;
            }

            long elapsedSec = (System.currentTimeMillis() - start) / 1000;
            if (elapsedSec > LOGIN_TIMEOUT_SEC) {
                System.out.println("⚠️ Не дождались логина за " + LOGIN_TIMEOUT_SEC +
                        " сек. Продолжаем как гость (документы могут быть пустыми).");
                return;
            }

            System.out.println("⏳ Жду логин... осталось ~" + (LOGIN_TIMEOUT_SEC - elapsedSec) + " сек.");
            try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    // =========================
    // ПОИСК ССЫЛОК НА ЛОТЫ
    // =========================
    private static List<WebElement> findLotLinks(WebDriver driver) {
        String[] selectors = {
                "a.search-card__heading",
                "a[href*='/lot/']",
                "a.search-card",
                "a.card a[href*='/lot/']",
                "a.card__link"
        };

        for (String css : selectors) {
            List<WebElement> els = driver.findElements(By.cssSelector(css));
            List<WebElement> links = new ArrayList<>();
            for (WebElement e : els) {
                String href = e.getAttribute("href");
                if (href != null && href.contains("/lot/")) links.add(e);
            }
            if (!links.isEmpty()) {
                System.out.println("FOUND by selector: " + css + " -> " + links.size());
                return links;
            }
        }
        return Collections.emptyList();
    }

    // =========================
    // ПАРСИНГ СТРАНИЦЫ ЛОТА
    // =========================
    private static LotData parseLotPage(WebDriver driver) {
        LotData data = new LotData();

        data.setLotUrl(driver.getCurrentUrl());

        // Лот №..., торги №...
        String headerText = safeXpathText(driver,
                "//span[contains(@class, 'lot__help') and contains(text(), 'Лот №')]");
        if (!headerText.isBlank()) {
            String[] parts = headerText.split(",");
            if (parts.length > 0) data.setLotNumber(parts[0].trim().replace("Лот №", "").trim());
            if (parts.length > 1) data.setAuctionNumber(parts[1].trim().replace("торги №", "").trim());
        }
// === Окончание приема заявок ===
        String appEndRaw = safeXpathText(driver,
                "//*[contains(text(),'Приём заявок до')]/following::*[1]");

        data.setApplicationEndDate(parseDateTime(appEndRaw));
        data.setAddress(extractAddress(driver));

        data.setStartPrice(safeXpathText(driver,
                "//span[contains(@class,'lot-info__subtitle') and contains(text(),'Начальная')]/following-sibling::span//span[not(@class)]"));

        data.setBidStep(safeXpathText(driver,
                "//span[contains(@class,'lot-info__subtitle') and contains(text(),'Шаг повышения')]/following-sibling::span"));

        data.setDepositAmount(safeXpathText(driver,
                "//span[contains(@class,'lot-info__subtitle') and contains(text(),'Задаток')]/following-sibling::div"));

        String tradeStartRaw = safeXpathText(driver,
                "//span[contains(@class,'lot-info__subtitle') and contains(text(),'Дата проведения')]/following-sibling::span");
        data.setStartAuc(parseDateTime(tradeStartRaw));

        // ✅ ДАТУ ОКОНЧАНИЯ ТОРГОВ МЫ БОЛЬШЕ НЕ ИЩЕМ И НЕ ЗАПОЛНЯЕМ (по твоей просьбе)
        // data.setTradeEndDateTime(...);  <-- удалено/не используется

        // ✅ документы (после логина обычно появляются)
        data.setDocumentationUrl(extractDocsUrls(driver));

        data.setAuctionStatus(extractStatus(driver));

        // описание: берём h1
        data.setDescription(safeCssText(driver, "h1"));

        // должник: пробуем открыть страницу должника
        String debtorUrl = findDebtorPageUrl(driver);
        data.setDebtorInfo(parseDebtorInfoFromDebtorPage(driver, debtorUrl));

        // чистим всё от null/н/д/-
        data.setLotNumber(clean(data.getLotNumber()));
        data.setAuctionNumber(clean(data.getAuctionNumber()));
        data.setAddress(clean(data.getAddress()));
        data.setStartPrice(clean(data.getStartPrice()));
        data.setBidStep(clean(data.getBidStep()));
        data.setDepositAmount(clean(data.getDepositAmount()));
        data.setDocumentationUrl(clean(data.getDocumentationUrl()));
        data.setAuctionStatus(clean(data.getAuctionStatus()));
        data.setDebtorInfo(clean(data.getDebtorInfo()));
        data.setDescription(clean(data.getDescription()));

        return data;
    }

    // =========================
    // ПЕЧАТЬ ТЗ (БЕЗ ДАТЫ ОКОНЧАНИЯ)
    // =========================
    private static void printTz10(LotData lot) {
        System.out.println("===== ТЗ (без даты окончания) =====");
        System.out.println("1) Номер аукциона / лота: " + clean(joinAuctionLot(lot)));
        System.out.println("2) Адрес объекта: " + clean(lot.getAddress()));
        System.out.println("3) Начальная цена: " + clean(lot.getStartPrice()));
        System.out.println("4) Шаг аукциона: " + clean(lot.getBidStep()));
        System.out.println("5) Размер задатка: " + clean(lot.getDepositAmount()));
        System.out.println("6) Дата/время начала торгов: " + fmt(lot.getStartAuc()));
        System.out.println("7) Ссылка на документацию: " + clean(lot.getDocumentationUrl()));
        System.out.println("8) Статус аукциона: " + clean(lot.getAuctionStatus()));
        System.out.println("9) Информация о должнике: " + clean(lot.getDebtorInfo()));
        System.out.println("10) Описание объекта: " + clean(lot.getDescription()));
        System.out.println("===================================");
    }

    private static String joinAuctionLot(LotData lot) {
        String lotNum = clean(lot.getLotNumber());
        String aucNum = clean(lot.getAuctionNumber());

        String left = lotNum.isEmpty() ? "" : ("Лот №" + lotNum);
        String right = aucNum.isEmpty() ? "" : ("Торги №" + aucNum);

        if (!left.isEmpty() && !right.isEmpty()) return left + " / " + right;
        if (!left.isEmpty()) return left;
        return right;
    }

    private static String fmt(LocalDateTime dt) {
        if (dt == null) return "";
        return dt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }

    // =========================
    // ДОЛЖНИК: ССЫЛКА
    // =========================
    private static String findDebtorPageUrl(WebDriver driver) {
        try {
            List<WebElement> links = driver.findElements(By.cssSelector("a[href*='/bankrot/']"));
            for (WebElement a : links) {
                String href = a.getAttribute("href");
                if (href != null && href.contains("/bankrot/")) return href;
            }
        } catch (Exception ignore) {}
        return "";
    }

    // =========================
    // ЧИСТКА: null / - / н/д
    // =========================
    private static String clean(String s) {
        if (s == null) return "";
        String x = s.trim();
        if (x.equalsIgnoreCase("null")) return "";
        if (x.equals("-")) return "";
        if (x.equalsIgnoreCase("н/д")) return "";
        if (x.equalsIgnoreCase("н/б")) return "";
        return x;
    }

    // =========================
    // HELPERS: XPATH/CSS/TEXT
    // =========================
    private static String safeXpathText(WebDriver driver, String xpath) {
        try {
            WebElement element = driver.findElement(By.xpath(xpath));
            return normalize(element.getAttribute("textContent"));
        } catch (Exception e) {
            return "";
        }
    }

    private static String safeCssText(WebDriver driver, String css) {
        try {
            WebElement element = driver.findElement(By.cssSelector(css));
            return normalize(element.getText());
        } catch (Exception e) {
            return "";
        }
    }

    private static String safeMeta(WebDriver driver, String cssSelector) {
        try {
            WebElement meta = driver.findElement(By.cssSelector(cssSelector));
            return normalize(meta.getAttribute("content"));
        } catch (Exception e) {
            return "";
        }
    }

    private static String normalize(String s) {
        if (s == null) return "";
        return s.replace((char) 160, ' ').trim().replaceAll("\\s+", " ");
    }

    private static String firstNonBlank(String... parts) {
        for (String p : parts) if (p != null && !p.isBlank()) return p.trim();
        return "";
    }

    // =========================
    // ADDRESS
    // =========================
    private static String extractAddress(WebDriver driver) {
        String content = firstNonBlank(
                safeMeta(driver, "meta[name='description']"),
                safeMeta(driver, "meta[property='og:description']")
        );

        String fromMeta = extractAfterMarker(content, "по адресу:");
        if (!fromMeta.isBlank()) return cleanupTail(fromMeta);

        String h1 = safeCssText(driver, "h1");
        String fromH1 = extractAfterMarker(h1, "по адресу:");
        if (!fromH1.isBlank()) return cleanupTail(fromH1);

        return "";
    }

    private static String extractAfterMarker(String text, String marker) {
        if (text == null) return "";
        String lower = text.toLowerCase(Locale.ROOT);
        int i = lower.indexOf(marker.toLowerCase(Locale.ROOT));
        if (i < 0) return "";
        return normalize(text.substring(i + marker.length()));
    }

    private static String cleanupTail(String s) {
        String x = s;
        x = x.replaceAll("\\s+в\\s+категории\\s+.*$", "").trim();
        x = x.replaceAll("\\s+на\\s+торгах\\s+по\\s+банкротству.*$", "").trim();
        x = x.replaceAll(",\\s*в\\s+[^,]+$", "").trim();
        return x;
    }

    // =========================
    // DOCS URLS (УЛУЧШЕНО: ловим и без расширений)
    // =========================
    private static String extractDocsUrls(WebDriver driver) {
        LinkedHashSet<String> links = new LinkedHashSet<>();
        try {
            for (WebElement a : driver.findElements(By.cssSelector("a[href]"))) {
                String href = a.getAttribute("href");
                if (href == null) continue;
                String h = href.toLowerCase(Locale.ROOT);

                boolean byExt = h.endsWith(".pdf") || h.endsWith(".doc") || h.endsWith(".docx")
                        || h.endsWith(".xls") || h.endsWith(".xlsx")
                        || h.endsWith(".zip") || h.endsWith(".rar");

                boolean byKeyword = h.contains("download") || h.contains("file")
                        || h.contains("document") || h.contains("docs")
                        || h.contains("attachment") || h.contains("doc");

                if (byExt || byKeyword) links.add(href);
            }
        } catch (Exception ignore) {}
        return String.join("; ", links);
    }

    // =========================
    // STATUS
    // =========================
    private static String extractStatus(WebDriver driver) {
        String status = firstNonBlank(
                safeCssText(driver, ".lot__status"),
                safeCssText(driver, ".lot-status"),
                safeCssText(driver, ".status"),
                safeCssText(driver, ".badge"),
                safeCssText(driver, ".chip")
        );
        status = normalize(status);
        if (status.length() > 80) status = status.substring(0, 80).trim();
        return status;
    }

    // =========================
    // DEBTOR PAGE PARSER
    // =========================
    private static String parseDebtorInfoFromDebtorPage(WebDriver driver, String debtorUrl) {
        if (debtorUrl == null || debtorUrl.isBlank()) {
            return clean(extractDebtorFromLotPage(driver));
        }

        String originalHandle = driver.getWindowHandle();
        Set<String> beforeHandles = driver.getWindowHandles();

        try {
            ((JavascriptExecutor) driver).executeScript("window.open(arguments[0], '_blank');", debtorUrl);

            String newHandle = waitNewTab(driver, beforeHandles, 10);
            if (newHandle == null) return clean(extractDebtorFromLotPage(driver));

            driver.switchTo().window(newHandle);

            WebDriverWait w = new WebDriverWait(driver, Duration.ofSeconds(15));
            w.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("body")));

            String name = firstNonBlank(
                    safeCssText(driver, "h1"),
                    safeXpathText(driver, "//h1"),
                    safeXpathText(driver, "//*[contains(text(),'Должник')]/following::*[1]")
            );

            String inn = firstNonBlank(
                    valueAfterLabel(driver, "ИНН"),
                    valueAfterLabel(driver, "Inn"),
                    safeXpathText(driver, "//*[contains(text(),'ИНН')]/following::*[1]")
            );

            String ogrn = firstNonBlank(
                    valueAfterLabel(driver, "ОГРН"),
                    valueAfterLabel(driver, "Ogrn")
            );

            String snils = firstNonBlank(
                    valueAfterLabel(driver, "СНИЛС"),
                    valueAfterLabel(driver, "Snils")
            );

            String region = firstNonBlank(
                    valueAfterLabel(driver, "Регион"),
                    valueAfterLabel(driver, "Region")
            );

            String caseNumber = firstNonBlank(
                    valueAfterLabel(driver, "Номер дела"),
                    valueAfterLabel(driver, "Дело"),
                    valueAfterLabel(driver, "Case")
            );

            List<String> parts = new ArrayList<>();
            if (!clean(name).isEmpty()) parts.add(clean(name));
            if (!clean(inn).isEmpty()) parts.add("ИНН: " + clean(inn));
            if (!clean(ogrn).isEmpty()) parts.add("ОГРН: " + clean(ogrn));
            if (!clean(snils).isEmpty()) parts.add("СНИЛС: " + clean(snils));
            if (!clean(region).isEmpty()) parts.add("Регион: " + clean(region));
            if (!clean(caseNumber).isEmpty()) parts.add("Дело: " + clean(caseNumber));

            return String.join(", ", parts);

        } catch (Exception e) {
            return clean(extractDebtorFromLotPage(driver));

        } finally {
            try {
                Set<String> now = driver.getWindowHandles();
                for (String h : now) {
                    if (!h.equals(originalHandle)) {
                        driver.switchTo().window(h);
                        driver.close();
                    }
                }
            } catch (Exception ignore) {}

            try { driver.switchTo().window(originalHandle); } catch (Exception ignore) {}
        }
    }

    private static String waitNewTab(WebDriver driver, Set<String> before, int timeoutSec) {
        long end = System.currentTimeMillis() + timeoutSec * 1000L;
        while (System.currentTimeMillis() < end) {
            Set<String> after = driver.getWindowHandles();
            if (after.size() > before.size()) {
                for (String h : after) if (!before.contains(h)) return h;
            }
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        }
        return null;
    }

    private static String extractDebtorFromLotPage(WebDriver driver) {
        return firstNonBlank(
                safeXpathText(driver, "//*[contains(text(),'Должник')]/following::*[1]"),
                safeXpathText(driver, "//*[contains(text(),'Должник')]/following-sibling::*[1]"),
                safeCssText(driver, ".debtor"),
                safeCssText(driver, ".lot__debtor"),
                safeCssText(driver, ".lot-debtor")
        );
    }

    private static String valueAfterLabel(WebDriver driver, String label) {
        if (label == null || label.isBlank()) return "";

        String x1 = safeXpathText(driver, "//*[normalize-space()='" + label + "']/following::*[1]");
        if (!x1.isBlank()) return x1;

        String x2 = safeXpathText(driver, "//*[contains(normalize-space(),'" + label + "')]/following::*[1]");
        if (!x2.isBlank()) return x2;

        return safeXpathText(driver, "//dt[contains(normalize-space(),'" + label + "')]/following-sibling::dd[1]");
    }

    // =========================
    // DATE PARSER
    // =========================
    private static LocalDateTime parseDateTime(String text) {
        if (text == null || text.isBlank()) return null;
        String t = normalize(text);

        try {
            return LocalDateTime.parse(t, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
        } catch (Exception ignore) {}

        try {
            return LocalDate.parse(t, DateTimeFormatter.ofPattern("dd.MM.yyyy")).atStartOfDay();
        } catch (Exception ignore) {}

        return null;
    }
}