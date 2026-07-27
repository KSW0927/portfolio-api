package com.demo.toy.comics;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ComicsUpdateDTO {

	@JsonProperty("comicsId")
	private Long comicsId;
	
    @JsonProperty("volume")
    private Integer volume;
    
    @JsonProperty("page")
    private Integer page;
    
    @JsonProperty("volumePrice")
    private BigDecimal volumePrice; 

    @JsonProperty("volumeImageUrl")
    private String volumeImageUrl;

    @JsonProperty("volumeFileSize")
    private String volumeFileSize;

	public Long getComicsId() {
		return comicsId;
	}

	public void setComicsId(Long comicsId) {
		this.comicsId = comicsId;
	}

	public Integer getVolume() {
		return volume;
	}

	public void setVolume(Integer volume) {
		this.volume = volume;
	}

	public Integer getPage() {
		return page;
	}

	public void setPage(Integer page) {
		this.page = page;
	}

	public BigDecimal getVolumePrice() {
		return volumePrice;
	}

	public void setVolumePrice(BigDecimal volumePrice) {
		this.volumePrice = volumePrice;
	}

	public String getVolumeImageUrl() {
		return volumeImageUrl;
	}

	public void setVolumeImageUrl(String volumeImageUrl) {
		this.volumeImageUrl = volumeImageUrl;
	}

	public String getVolumeFileSize() {
		return volumeFileSize;
	}

	public void setVolumeFileSize(String volumeFileSize) {
		this.volumeFileSize = volumeFileSize;
	}
}