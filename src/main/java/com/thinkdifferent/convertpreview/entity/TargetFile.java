package com.thinkdifferent.convertpreview.entity;

import lombok.Data;

import java.io.File;

@Data
public class TargetFile {

    private File target;

    private long longPageCount = 0;

}
