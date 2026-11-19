package me.noynto.eosa.collection;

import me.noynto.eosa.shared.CollectionId;
import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.JewelId;
import me.noynto.eosa.shared.VideoId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Une liste de bijoux et la vidéo qui ouvre la page d'accueil, nommée avec une saison et une année (« Été 2027 »). */
public class Collection {

    public static final int MIN_YEAR = 2024;
    public static final int MAX_YEAR = 2100;
    public static final int MAX_HEADLINE_LENGTH = 80;
    public static final int MAX_BLURB_LENGTH = 240;

    private CollectionId id;
    private String name;
    private CollectionSeason season;
    private int year;
    private CollectionState state;
    private Instant publishedAt;
    private VideoId videoId;
    private ImageId posterImageId;
    private String headline;
    private String blurb;
    private List<JewelId> jewelIds = new ArrayList<>();

    public Collection() {
    }

    public CollectionId getId() {
        return id;
    }

    public void setId(CollectionId id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CollectionSeason getSeason() {
        return season;
    }

    public void setSeason(CollectionSeason season) {
        this.season = season;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public CollectionState getState() {
        return state;
    }

    public void setState(CollectionState state) {
        this.state = state;
    }

    /** Date de la dernière publication, qui désigne la collection du hero ; null tant que la collection n'a jamais été publiée. */
    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public VideoId getVideoId() {
        return videoId;
    }

    public void setVideoId(VideoId videoId) {
        this.videoId = videoId;
    }

    /** Image affichée pendant le chargement de la vidéo et quand le visiteur demande moins de mouvement. */
    public ImageId getPosterImageId() {
        return posterImageId;
    }

    public void setPosterImageId(ImageId posterImageId) {
        this.posterImageId = posterImageId;
    }

    /** Titre d'accroche du hero ; null quand l'administratrice n'en a pas écrit. */
    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    /** Court paragraphe sous le titre du hero ; null quand il n'y en a pas. */
    public String getBlurb() {
        return blurb;
    }

    public void setBlurb(String blurb) {
        this.blurb = blurb;
    }

    public List<JewelId> getJewelIds() {
        return jewelIds;
    }

    public void setJewelIds(List<JewelId> jewelIds) {
        this.jewelIds = jewelIds;
    }
}
