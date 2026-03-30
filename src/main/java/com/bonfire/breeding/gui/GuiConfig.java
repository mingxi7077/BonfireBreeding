package com.bonfire.breeding.gui;

import com.google.gson.JsonObject;

/**
 * Configurable GUI layout and titles.
 * Supports plain text titles and ItemsAdder font_image symbols.
 */
public class GuiConfig {
    private String menuTitle = "牧场管理菜单";
    private String menuTitleFontImage = null;
    private int menuRows = 3;

    private String detailTitlePrefix = "";
    private String detailTitleFontImage = null;

    private String listTitleFontImage = null;

    public static GuiConfig fromJson(JsonObject json) {
        GuiConfig gc = new GuiConfig();
        if (json == null) return gc;

        if (json.has("menu")) {
            JsonObject menu = json.getAsJsonObject("menu");
            gc.menuTitle = menu.has("title") ? menu.get("title").getAsString() : gc.menuTitle;
            gc.menuTitleFontImage = menu.has("font_image") ? menu.get("font_image").getAsString() : null;
            gc.menuRows = menu.has("rows") ? menu.get("rows").getAsInt() : 3;
        }

        if (json.has("detail")) {
            JsonObject detail = json.getAsJsonObject("detail");
            gc.detailTitlePrefix = detail.has("title_prefix") ? detail.get("title_prefix").getAsString() : "";
            gc.detailTitleFontImage = detail.has("font_image") ? detail.get("font_image").getAsString() : null;
        }

        if (json.has("list")) {
            JsonObject list = json.getAsJsonObject("list");
            gc.listTitleFontImage = list.has("font_image") ? list.get("font_image").getAsString() : null;
        }

        return gc;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        JsonObject menu = new JsonObject();
        menu.addProperty("title", menuTitle);
        if (menuTitleFontImage != null) menu.addProperty("font_image", menuTitleFontImage);
        menu.addProperty("rows", menuRows);
        json.add("menu", menu);

        JsonObject detail = new JsonObject();
        detail.addProperty("title_prefix", detailTitlePrefix);
        if (detailTitleFontImage != null) detail.addProperty("font_image", detailTitleFontImage);
        json.add("detail", detail);

        JsonObject list = new JsonObject();
        if (listTitleFontImage != null) list.addProperty("font_image", listTitleFontImage);
        json.add("list", list);

        return json;
    }

    /**
     * Resolve the title to display. If font_image is set, prepend the symbol character.
     */
    public String resolveMenuTitle() {
        if (menuTitleFontImage != null && !menuTitleFontImage.isEmpty()) {
            return menuTitleFontImage;
        }
        return menuTitle;
    }

    public String resolveDetailTitle(String animalDisplayName) {
        if (detailTitleFontImage != null && !detailTitleFontImage.isEmpty()) {
            return detailTitleFontImage.replace("%name%", animalDisplayName);
        }
        return detailTitlePrefix + animalDisplayName;
    }

    public String resolveListTitle(String label, String animalDisplayName) {
        if (listTitleFontImage != null && !listTitleFontImage.isEmpty()) {
            return listTitleFontImage.replace("%label%", label).replace("%name%", animalDisplayName);
        }
        return label + " - " + animalDisplayName;
    }

    public int getMenuRows() { return menuRows; }
    public String getMenuTitle() { return menuTitle; }
}
