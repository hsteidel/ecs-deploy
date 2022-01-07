package com.hxs.ecsdeploy.commands;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateServiceOptions {

    private String tag;

    private String region;

    private String cluster;

    private String service;

}
