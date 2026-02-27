package auccsion;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

public class SeleniumTest {
    public static void main(String[] args) throws InterruptedException {
        WebDriverManager.chromedriver().setup();
        WebDriver driver = new ChromeDriver();

        driver.get("https://bankrotbaza.ru/search?comb=all&category%5B%5D=27&type_auction=on&sort=created_desc");

    }
}







//•	Номер аукциона / лота
//•	Торги
//•	Адрес объекта(Его адрес как то сложно достать займёмся с этим потом)
//•	Начальная цена
//•	Шаг повышение
//•	Размер задатка
//•	Прием заявок начала
//•	Окончания заявок
//•	Ссылка на документацию (сделаем ссылку на саму страницу, а именно ту аукцион)
//•	Категории
//•	Информация о должнике
//•	Наименование / ФИО (Организатор)
//•	ИНН (Организатор)
//•	Контактное лицо
//•	Контактный телефон
//•	Наименование / ФИО(Продавец)
//•	ИНН (Продавец)
//•	Контактный телефон(Продавец)
//•	Описание объекта (площадь, этаж и т.д.)

