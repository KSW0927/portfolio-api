package com.demo.toy.comics;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.demo.toy.contents.ContentsEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "COMICS")
public class ComicsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comics_id", nullable = false, updatable = false)
    private Long comicsId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", nullable = false)
    private ContentsEntity content;

    @Column(nullable = false)
    private Integer volume;

    @Column(nullable = false)
    private Integer page;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal volumePrice = BigDecimal.ZERO;

    @Column(length = 300)
    private String volumeImageUrl;

    @Column(length = 50)
    private String volumeFileSize;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime regDate;
    
    // 등록
    public static ComicsEntity create(
            ContentsEntity content,
            Integer volume,
            Integer page,
            BigDecimal volumePrice,
            String volumeImageUrl,
            String volumeFileSize
        ) {
            ComicsEntity entity = new ComicsEntity();
            entity.content = content;
            entity.volume = volume;
            entity.page = page;
            entity.volumePrice =
                volumePrice != null ? volumePrice : BigDecimal.ZERO;
            entity.volumeImageUrl = volumeImageUrl;
            entity.volumeFileSize = volumeFileSize;
            return entity;
        }
    
    // 수정
    public void update(ComicsUpdateDTO dto) {
        this.volume = dto.getVolume();
        this.page = dto.getPage();
        this.volumePrice = dto.getVolumePrice() != null ? dto.getVolumePrice() : BigDecimal.ZERO;
        this.volumeImageUrl = dto.getVolumeImageUrl();
        this.volumeFileSize = dto.getVolumeFileSize();
    }

	public Long getComicsId() {
		return comicsId;
	}

	public void setComicsId(Long comicsId) {
		this.comicsId = comicsId;
	}

	public ContentsEntity getContent() {
		return content;
	}

	public void setContent(ContentsEntity content) {
		this.content = content;
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

	public LocalDateTime getRegDate() {
		return regDate;
	}

	public void setRegDate(LocalDateTime regDate) {
		this.regDate = regDate;
	}
}
