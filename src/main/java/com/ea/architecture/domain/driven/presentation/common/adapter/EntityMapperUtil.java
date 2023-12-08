package com.ea.architecture.domain.driven.presentation.common.adapter;

import com.ea.architecture.domain.driven.domain.common.model.UniqueId;

public interface EntityMapperUtil {
    default long map(UniqueId uid) { return uid.getId();}
    default UniqueId map(long uid) { return new UniqueId(uid);}
}
