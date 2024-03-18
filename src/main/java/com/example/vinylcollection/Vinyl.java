package com.example.vinylcollection;

public class Vinyl {

    private String artistName;
    private String albumTitle;
    private int releaseYear;
    private String genre;
    private String size;
    private String condition;

    public Vinyl(String artistName, String albumTitle, int releaseYear, String genre, String size, String condition) {
        this.artistName = artistName;
        this.albumTitle = albumTitle;
        this.releaseYear = releaseYear;
        this.genre = genre;
        this.size = size;
        this.condition = condition;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public String getAlbumTitle() {
        return albumTitle;
    }

    public void setAlbumTitle(String albumTitle) {
        this.albumTitle = albumTitle;
    }

    public int getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(int releaseYear) {
        this.releaseYear = releaseYear;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }
}
