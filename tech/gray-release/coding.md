## v1
```yaml
features:
- key: call_newapi_getUserById
  enabled: true
  rule: {893,342,1020-1120,%30}
- key: call_newapi_registerUser
  enabled: true
  rule: {1391198723, %10}
- key: newalgo_loan
  enabled: true
  rule: {0-1000}
```

```java
public class DarkLaunch {
    private static final Logger log = LoggerFactory.getLogger(DarkLaunch.class);
    private static final int DEFAULT_RULE_UPDATE_TIME_INTERVAL = 60;
    private DarkRule rule;
    private ScheduledExecutorService executor;

    public DarkLaunch(int ruleUpdateTimeInterval) {
        loadRule();
        this.executor = Executors.newSingleThreadScheduledExecutor();
        this.executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                loadRule();
            }
        }, ruleUpdateTimeInterval, ruleUpdateTimeInterval, TimeUnit.SECONDS);
    }

    public DarkLaunch() {
        this(DEFAULT_RULE_UPDATE_TIME_INTERVAL);
    }

    private void loadRule() {
        InputStream in = null;
        DarkRuleConfig ruleConfig = null;

        try {
            in = this.getClass().getResourceAsStream("/dark-rule.yaml");

            if (in != null) {
                Yaml yaml = new Yaml();
                ruleConfig = yaml.loadAs(in, DarkRuleConfig.class);
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    log.error("close file error:{}", ex);
                }
            }
        }

        if (ruleConfig == null) {
            throw new RuntimeException("Can not load dark rule.");
        }

        DarkRule newRule = new DarkRule(ruleConfig);
        this.rule = newRule;
    }

    public DarkFeature getDarkFeature(String featureKey) {
        DarkFeature darkFeature = this.rule.getDarkFeature(featureKey);
        return darkFeature;
    }
}
```

```java
public class DarkRuleConfig {
    private List<DarkFeatureConfig> features;

    public List<DarkFeatureConfig> getFeatures() {
        return this.features;
    }

    public void setFeatures(List<DarkFeatureConfig> features) {
        this.features = features;
    }

    public static class DarkFeatureConfig {
        private String key;
        private boolean enabled;
        private String rule;
    }
}
```

```java
public class DarkRule {
    private Map<String, DarkFeature> darkFeatures = new HashMap<>();

    public DarkRule(DarkRuleConfig darkRuleConfig) {
        List<DarkRuleConfig.DarkFeatureConfig> darkFeatureConfigs = darkRuleConfig.getFeatures();
        for (DarkRuleConfig.DarkFeatureConfig darkFeatureConfig : darkFeatureConfigs) {
            darkFeatures.put(darkFeatureConfig.getKey(), new DarkFeature(darkFeatureConfig));
        }
    }

    public DarkFeature getDarkFeature(String featureKey) {
        return darkFeatures.get(featureKey);
    }
}
```

```java
public class DarkFeature {
    private String key;
    private boolean enabled;
    private int percentage;
    private RangeSet<Long> rangeSet = TreeRangeSet.create();

    public DarkFeature(DarkRuleConfig.DarkFeatureConfig darkFeatureConfig) {
        this.key = darkFeatureConfig.getKey();
        this.enabled = darkFeatureConfig.getEnabled();
        String darkRule = darkFeatureConfig.getRule().trim();
        parseDarkRule(darkRule);
    }

    protected void parseDarkRule(String darkRule) {
        if (!darkRule.startsWith("{") || !darkRule.endsWith("}")) {
            throw new RuntimeException("Failed to parse dark rule: " + darkRule);
        }

        String[] rules = darkRule.substring(1, darkRule.length() - 1).split(",");
        this.rangeSet.clear();
        this.percentage = 0;

        for (String rule : rules) {
            rule = rule.trim();

            if (StringUtils.isEmpty(rule)) {
                continue;
            }

            if (rule.startsWith("%")) {
                int newPercentage = Integer.parseInt(rule.substring(1));

                if (newPercentage > this.percentage) {
                    this.percentage = newPercentage;
                }
            } else if (rule.contains("-")) {
                String[] parts = rule.split("-");

                if (parts.length != 2) {
                    throw new RuntimeException("Failed to parse dark rule: " + darkRule);
                }

                long start = Long.parseLong(parts[0]);
                long end = Long.parseLong(parts[1]);

                if (start > end) {
                    throw new RuntimeException("Failed to parse dark rule: " + darkRule);
                }

                this.rangeSet.add(Range.closed(start, end));
            } else {
                long val = Long.parseLong(rule);
                this.rangeSet.add(Range.closed(val, val));
            }
        }
    }

    public boolean enabled() {
        return this.enabled;
    }

    public boolean dark(long darkTarget) {
        boolean selected = this.rangeSet.contains(darkTarget);

        if (selected) {
            return true;
        }

        long reminder = darkTarget % 100;

        if (reminder >= 0 && reminder < this.percentage) {
            return true;
        }

        return false;
    }

    public boolean dark(String darkTarget) {
        long target = Long.parseLong(darkTarget);
        return dark(target);
    }
}
```


## v2
实现基于编程的灰度规则配置方式

```java
public interface IDarkFeature {
    boolean enabled();
    boolean dark(long darkTarget);
    boolean dark(String darkTarget);
}
```

基于这个抽象接口，业务系统可以自己编程实现复杂的灰度规则，然后添加到 DarkRule 中。为了避免配置文件中的灰度规则热更新时，覆盖掉编程实现的灰度规则，在 DarkRule 中，我们对从配置文件中加载的灰度规则和编程实现的灰度规则分开存储。按照这个设计思 路，我们对 DarkRule 类进行重构。重构之后的代码如下所示:

```java
public class DarkRule {
    private Map<String, IDarkFeature> darkFeatures = new HashMap<>();
    private ConcurrentHashMap<String, IDarkFeature> programmedDarkFeatures = new ConcurrentHashMap<String, IDarkFeature>();
    
    public void addProgrammedDarkFeature(String featureKey, IDarkFeature darkFeature) {
        programmedDarkFeatures.put(featureKey, darkFeature);
    }

    public void setDarkFeatures(Map<String, IDarkFeature> newDarkFeatures) {
        this.darkFeatures = newDarkFeatures;
    }

    public IDarkFeature getDarkFeature(String featureKey) {
        IDarkFeature darkFeature = programmedDarkFeatures.get(featureKey);

        if (darkFeature != null) {
            return darkFeature;
        }

        return darkFeatures.get(featureKey);
    }
}
```

```java
public class DarkLaunch {
    private static final Logger log = LoggerFactory.getLogger(DarkLaunch.class);
    private static final int DEFAULT_RULE_UPDATE_TIME_INTERVAL = 60;
    private DarkRule rule = new DarkRule();
    private ScheduledExecutorService executor;

    public DarkLaunch(int ruleUpdateTimeInterval) {
        loadRule();
        this.executor = Executors.newSingleThreadScheduledExecutor();
        this.executor.scheduleAtFixedRate(new Runnable() {
            public void run() {
                loadRule();
            }
        }, ruleUpdateTimeInterval, ruleUpdateTimeInterval, TimeUnit.SECONDS);
    }

    public DarkLaunch() {
        this(DEFAULT_RULE_UPDATE_TIME_INTERVAL);
    }

    private void loadRule() {
        InputStream in = null;
        DarkRuleConfig ruleConfig = null;

        try {
            in = this.getClass().getResourceAsStream("/dark-rule.yaml");

            if (in != null) {
                Yaml yaml = new Yaml();
                ruleConfig = yaml.loadAs(in, DarkRuleConfig.class);
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    log.error("close file error:{}", e);
                }
            }
        }

        if (ruleConfig == null) {
            throw new RuntimeException("Can not load dark rule.");
        }

        Map<String, IDarkFeature> darkFeatures = new HashMap<>();
        List<DarkRuleConfig.DarkFeatureConfig> darkFeatureConfigs = ruleConfig.getFeatures();
        for (DarkRuleConfig.DarkFeatureConfig darkFeatureConfig : darkFeatureConfigs) {
            darkFeatures.put(darkFeatureConfig.getKey(), new DarkFeature(darkFeatureConfig));
        }

        this.rule.setDarkFeatures(darkFeatures);
    }

    public void addProgrammedDarkFeature(String featureKey, IDarkFeature darkFeature) {
        this.rule.addProgrammedDarkFeature(featureKey, darkFeature);
    }

    public IDarkFeature getDarkFeature(String featureKey) {
        IDarkFeature darkFeature = this.rule.getDarkFeature(featureKey);
        return darkFeature;
    }
}
```

```java
public class UserPromotionDarkRule implements IDarkFeature {
    public boolean enabled() {
        return true;
    }

    public boolean dark(long darkTarget) {
        return false;
    }

    public boolean dark(String darkTarget) {
        return false;
    }
}
```