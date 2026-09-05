package com.volt.module.setting;

import lombok.*;

@Setter
@Getter
public abstract class Setting {
    private String name;

    public Setting(String name) {
        this.name = name;
    }
}
