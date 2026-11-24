package com.example.quiz.entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;

@MappedSuperclass
@Getter
@Setter
@ToString
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        BaseEntity that = (BaseEntity) o;
        return getId() != null && getId().equals(that.getId());
    }

    @Override
    public final int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}