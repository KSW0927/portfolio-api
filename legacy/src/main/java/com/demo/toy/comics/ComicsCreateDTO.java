package com.demo.toy.comics;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ComicsCreateDTO {

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

    public ComicsCreateDTO() {}

    public ComicsCreateDTO (Integer volume, Integer page, 
    		BigDecimal volumePrice, String volumeImageUrl,String volumeFileSize) {
    	this.volume = volume;
        this.page = page;
        this.volumePrice = volumePrice;
        this.volumeImageUrl = volumeImageUrl;
        this.volumeFileSize = volumeFileSize;
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