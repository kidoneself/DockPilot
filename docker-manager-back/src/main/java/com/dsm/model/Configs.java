package com.dsm.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class Configs {


    @JsonProperty("target")
    private String target;

    @JsonProperty("urls")
    private List<String> urls;


}
