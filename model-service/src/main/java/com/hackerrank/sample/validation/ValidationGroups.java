package com.hackerrank.sample.validation;

public final class ValidationGroups {
    private ValidationGroups() {
    }

    public interface Create {
    }

    public interface Update {
    }

    public interface CreateOrUpdate extends Create, Update {
    }
}
