package com.pain.design;

public enum EnumSingleton {
    INSTANCE {
        protected void doSth() {
            System.out.println("do something");
        }
    };

    // 反编译查看
    protected abstract void doSth();

    private Object data;

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public static EnumSingleton getInstance() {
        return INSTANCE;
    }
}
