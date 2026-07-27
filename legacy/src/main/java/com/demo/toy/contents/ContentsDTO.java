package com.demo.toy.contents;

import java.math.BigDecimal;
import com.demo.toy.common.response.ContentType; // ContentType Enum 임포트
import com.fasterxml.jackson.annotation.JsonProperty;

public class ContentsDTO {

    @JsonProperty("contentId")
    private Long contentId; 

    @JsonProperty("contentType")
    private ContentType contentType; 

    @JsonProperty("title")
    private String title;

    @JsonProperty("coverImageUrl")
    private String coverImageUrl;

    @JsonProperty("price")
    private BigDecimal price; 

    @JsonProperty("authorId")
    private Long authorId; 

    @JsonProperty("description")
    private String description;

    @JsonProperty("ratingAvg")
    private BigDecimal ratingAvg;

    @JsonProperty("isAdult")
    private Boolean isAdult;
    
    @JsonProperty("regDate")
    private String regDate;

    public ContentsDTO() {}

    public ContentsDTO(Long contentId, ContentType contentType, String title, 
                  String coverImageUrl, BigDecimal price, Long authorId, 
                  String description, BigDecimal ratingAvg, Boolean isAdult, String regDate) {
        this.contentId = contentId;
        this.contentType = contentType;
        this.title = title;
        this.coverImageUrl = coverImageUrl;
        this.price = price;
        this.authorId = authorId;
        this.description = description;
        this.ratingAvg = ratingAvg;
        this.isAdult = isAdult;
        this.regDate = regDate;
    }

	public Long getContentId() {
		return contentId;
	}

	public void setContentId(Long contentId) {
		this.contentId = contentId;
	}

	public ContentType getContentType() {
		return contentType;
	}

	public void setContentType(ContentType contentType) {
		this.contentType = contentType;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getCoverImageUrl() {
		return coverImageUrl;
	}

	public void setCoverImageUrl(String coverImageUrl) {
		this.coverImageUrl = coverImageUrl;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public Long getAuthorId() {
		return authorId;
	}

	public void setAuthorId(Long authorId) {
		this.authorId = authorId;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public BigDecimal getRatingAvg() {
		return ratingAvg;
	}

	public void setRatingAvg(BigDecimal ratingAvg) {
		this.ratingAvg = ratingAvg;
	}

	public Boolean getIsAdult() {
		return isAdult;
	}

	public void setIsAdult(Boolean isAdult) {
		this.isAdult = isAdult;
	}

	public String getRegDate() {
		return regDate;
	}

	public void setRegDate(String regDate) {
		this.regDate = regDate;
	}

	@Override
	public String toString() {
		return "ApiDTO [contentId=" + contentId + ", contentType=" + contentType + ", title=" + title
				+ ", coverImageUrl=" + coverImageUrl + ", price=" + price + ", authorId=" + authorId + ", description="
				+ description + ", ratingAvg=" + ratingAvg + ", isAdult=" + isAdult + ", regDate=" + regDate + "]";
	}
}