package com.demo.toy.contents;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import com.demo.toy.comics.ComicsEntity;
import com.demo.toy.common.response.ContentType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "CONTENTS")
public class ContentsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "content_id", nullable = false, updatable = false)
    private Long contentId; 
    
    @Column(length = 255, nullable = false)
    private String title; 
    
    private String coverImageUrl; 
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContentType contentType; 

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal price = BigDecimal.ZERO; 
    
    private Long authorId; 
    
    @Lob
    private String description;
    
    @Column(precision = 3, scale = 2)
    private BigDecimal ratingAvg = BigDecimal.ZERO; 

    private Boolean isAdult = false; 
    
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime regDate;
    
    @OneToMany(
            mappedBy = "content", 			// 자식 entity에 설정된 부모 entity명
            cascade = CascadeType.REMOVE, 	// 부모 삭제 시 자식도 함께 삭제되도록 설정
            orphanRemoval = true, 			// 부모 컬렉션에서 제거만 해도 DB에서 삭제되게 하는 옵션
            fetch = FetchType.LAZY
        )
        private List<ComicsEntity> comicsList = new ArrayList<>();

    public ContentsEntity() {}

    public ContentsEntity(Long contentId, String title, String coverImageUrl, LocalDateTime regDate) {
        this.contentId = contentId;
        this.title = title;
        this.coverImageUrl = coverImageUrl;
        this.regDate = regDate;
        
        this.contentType = ContentType.BOOK;
        this.price = BigDecimal.ZERO;
        this.authorId = 0L;
    }
    
	public void update(ContentsDTO dto) {
        this.title = dto.getTitle();
        this.price = dto.getPrice();
        this.description = dto.getDescription();
        this.contentType = dto.getContentType();
        this.isAdult = dto.getIsAdult();
        this.coverImageUrl = dto.getCoverImageUrl();
    }

	public Long getContentId() {
		return contentId;
	}

	public void setContentId(Long contentId) {
		this.contentId = contentId;
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

	public ContentType getContentType() {
		return contentType;
	}

	public void setContentType(ContentType contentType) {
		this.contentType = contentType;
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

	public LocalDateTime getRegDate() {
		return regDate;
	}

	public void setRegDate(LocalDateTime regDate) {
		this.regDate = regDate;
	}
}
