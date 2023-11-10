package io.odh.test.e2e;

import io.odh.test.platform.KubeClient;
import io.odh.test.TestConstants;
import io.odh.test.separator.TestSeparator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class Abstract implements TestSeparator {
    protected KubeClient kubeClient;

    @BeforeAll
    void init() {
        kubeClient = new KubeClient(TestConstants.ODH_NAMESPACE);
    }
}
