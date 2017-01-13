package org.literacyapp.synchronization.jsonmodel;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

/**
 * Created by sladomic on 12.01.17.
 */

@JsonObject
public class JsonFileModel {

    @JsonField
    public byte[] byteArray;
}
