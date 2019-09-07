package net.mamoe.mirai.message.defaults;

import net.mamoe.mirai.message.FaceID;
import net.mamoe.mirai.message.Message;

/**
 * QQ 自带表情
 *
 * @author Him188moe
 */
public final class Face extends Message {
    private final FaceID id;

    public Face(FaceID id) {
        this.id = id;
    }

    public FaceID getId() {
        return id;
    }

    @Override
    public String toString() {
        // TODO: 2019/9/1
        throw new UnsupportedOperationException();
    }
}