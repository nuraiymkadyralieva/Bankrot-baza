package auction;

import java.time.LocalDateTime;

public class LotData {
    private String lotUrl;
    private String lotNumber;
    private String auctionNumber;
    private String category;
    private String address;

    private String startPrice;
    private String bidStep;
    private String depositAmount;

    private LocalDateTime applicationStartDate;
    private LocalDateTime applicationEndDate;

    private LocalDateTime start_Auc; // начало торгов

    private String documentationUrl;
    private String auctionStatus;
    private String debtorInfo;

    private String organizerName;
    private String organizerInn;
    private String sellerName;
    private String sellerPhone;

    private String description;

    public String getLotUrl() { return lotUrl; }
    public void setLotUrl(String lotUrl) { this.lotUrl = lotUrl; }

    public String getLotNumber() { return lotNumber; }
    public void setLotNumber(String lotNumber) { this.lotNumber = lotNumber; }

    public String getAuctionNumber() { return auctionNumber; }
    public void setAuctionNumber(String auctionNumber) { this.auctionNumber = auctionNumber; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getStartPrice() { return startPrice; }
    public void setStartPrice(String startPrice) { this.startPrice = startPrice; }

    public String getBidStep() { return bidStep; }
    public void setBidStep(String bidStep) { this.bidStep = bidStep; }

    public String getDepositAmount() { return depositAmount; }
    public void setDepositAmount(String depositAmount) { this.depositAmount = depositAmount; }

    public LocalDateTime getApplicationStartDate() { return applicationStartDate; }
    public void setApplicationStartDate(LocalDateTime applicationStartDate) { this.applicationStartDate = applicationStartDate; }

    public LocalDateTime getApplicationEndDate() { return applicationEndDate; }
    public void setApplicationEndDate(LocalDateTime applicationEndDate) { this.applicationEndDate = applicationEndDate; }

    // ✅ оставляем только начало торгов
    private LocalDateTime startAuc; // начало торгов

    public LocalDateTime getStartAuc() { return startAuc; }
    public void setStartAuc(LocalDateTime startAuc) { this.startAuc = startAuc; }

    public String getDocumentationUrl() { return documentationUrl; }
    public void setDocumentationUrl(String documentationUrl) { this.documentationUrl = documentationUrl; }

    public String getAuctionStatus() { return auctionStatus; }
    public void setAuctionStatus(String auctionStatus) { this.auctionStatus = auctionStatus; }

    public String getDebtorInfo() { return debtorInfo; }
    public void setDebtorInfo(String debtorInfo) { this.debtorInfo = debtorInfo; }

    public String getOrganizerName() { return organizerName; }
    public void setOrganizerName(String organizerName) { this.organizerName = organizerName; }

    public String getOrganizerInn() { return organizerInn; }
    public void setOrganizerInn(String organizerInn) { this.organizerInn = organizerInn; }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public String getSellerPhone() { return sellerPhone; }
    public void setSellerPhone(String sellerPhone) { this.sellerPhone = sellerPhone; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public String toString() {
        return "LotData{" +
                "lotUrl='" + lotUrl + '\'' +
                ", lotNumber='" + lotNumber + '\'' +
                ", auctionNumber='" + auctionNumber + '\'' +
                ", address='" + address + '\'' +
                ", startPrice='" + startPrice + '\'' +
                ", bidStep='" + bidStep + '\'' +
                ", depositAmount='" + depositAmount + '\'' +
                ", start_Auc=" + start_Auc +
                ", documentationUrl='" + documentationUrl + '\'' +
                ", auctionStatus='" + auctionStatus + '\'' +
                ", debtorInfo='" + debtorInfo + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}