/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.ml.inference.deployment;

import org.elasticsearch.license.LicensedFeature;
import org.elasticsearch.license.XPackLicenseState;
import org.elasticsearch.tasks.TaskId;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.core.ml.action.StartTrainedModelDeploymentAction;
import org.elasticsearch.xpack.core.ml.inference.trainedmodel.PassThroughConfig;
import org.elasticsearch.xpack.ml.inference.allocation.TrainedModelAllocationNodeService;

import java.util.Map;
import java.util.function.Consumer;

import static org.elasticsearch.xpack.core.ml.MlTasks.TRAINED_MODEL_ALLOCATION_TASK_NAME_PREFIX;
import static org.elasticsearch.xpack.core.ml.MlTasks.TRAINED_MODEL_ALLOCATION_TASK_TYPE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TrainedModelDeploymentTaskTests extends ESTestCase {

    void assertTrackingComplete(Consumer<TrainedModelDeploymentTask> method, String modelId) {
        XPackLicenseState licenseState = mock(XPackLicenseState.class);
        LicensedFeature.Persistent feature = mock(LicensedFeature.Persistent.class);
        TrainedModelDeploymentTask task = new TrainedModelDeploymentTask(
            0,
            TRAINED_MODEL_ALLOCATION_TASK_TYPE,
            TRAINED_MODEL_ALLOCATION_TASK_NAME_PREFIX + modelId,
            TaskId.EMPTY_TASK_ID,
            Map.of(),
            new StartTrainedModelDeploymentAction.TaskParams(
                modelId,
                randomLongBetween(1, Long.MAX_VALUE),
                randomInt(5),
                randomInt(5),
                randomInt(5)
            ),
            mock(TrainedModelAllocationNodeService.class),
            licenseState,
            feature
        );

        task.init(new PassThroughConfig(null, null, null));
        verify(feature, times(1)).startTracking(licenseState, "model-" + modelId);
        method.accept(task);
        verify(feature, times(1)).stopTracking(licenseState, "model-" + modelId);
    }

    public void testOnStopWithoutNotification() {
        assertTrackingComplete(t -> t.stopWithoutNotification("foo"), randomAlphaOfLength(10));
    }

    public void testOnStop() {
        assertTrackingComplete(t -> t.stop("foo"), randomAlphaOfLength(10));
    }

    public void testCancelled() {
        assertTrackingComplete(TrainedModelDeploymentTask::onCancelled, randomAlphaOfLength(10));
    }

}
