package com.demo.toy.comics;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ComicsResponseDTO {

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

    public static ComicsResponseDTO from(ComicsEntity entity) {
        ComicsResponseDTO dto = new ComicsResponseDTO();
        dto.comicsId = entity.getComicsId();
        dto.volume = entity.getVolume();
        dto.page = entity.getPage();
        dto.volumePrice = entity.getVolumePrice();
        dto.volumeImageUrl = entity.getVolumeImageUrl();
        dto.volumeFileSize = entity.getVolumeFileSize();
        return dto;
    }

    public Long getComicsId() {
        return comicsId;
    }
    
	public Integer getVolume() {
		return volume;
	}

	public Integer getPage() {
		return page;
	}

	public BigDecimal getVolumePrice() {
		return volumePrice;
	}

	public String getVolumeImageUrl() {
		return volumeImageUrl;
	}

	public String getVolumeFileSize() {
		return volumeFileSize;
	}
}