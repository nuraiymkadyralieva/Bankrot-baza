package auction;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        int maxLotsToParse = 200;

        String baseUrl =
                "https://bankrotbaza.ru/search?comb=all&category%5B%5D=27&type_auction=on&sort=created_desc";

        BankrotParser parser = new BankrotParser();
        List<LotData> lots = parser.parseLots(baseUrl, maxLotsToParse);

        ExcelExporter.export(lots, "bankrotbaza_lots_30.xlsx");

        System.out.println("\nГОТОВО. Собрано лотов: " + lots.size());
    }
}