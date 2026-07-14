package com.school.hei.endpoint.event.consumer.model;

import com.school.hei.PojaGenerated;
import com.school.hei.endpoint.event.model.PojaEvent;

@PojaGenerated
public record TypedEvent(String typeName, PojaEvent payload) {}
