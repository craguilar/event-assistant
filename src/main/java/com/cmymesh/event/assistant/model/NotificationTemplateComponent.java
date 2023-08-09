package com.cmymesh.event.assistant.model;


import lombok.ToString;

import java.util.List;

@ToString
public class NotificationTemplateComponent {
    public String type; // header , body , button
    public List<Parameter> parameters;

    @ToString
    public static class Parameter {

        public String type; // image,text
        public String text;
        public Image image;
        public Video video;
    }

    @ToString
    public static class Image {
        public String link;
    }

    @ToString
    public static class Video {
        public String link;
    }
}

