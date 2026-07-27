package com.demo.toy.contents;

public class ContentsSearchParamsDTO {

	private String title;
    private int page = 0;
    private int size = 9;

    public ContentsSearchParamsDTO() {}

    public ContentsSearchParamsDTO(String title, int page, int size) {
        this.title = title;
        this.page = page;
        this.size = size;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
