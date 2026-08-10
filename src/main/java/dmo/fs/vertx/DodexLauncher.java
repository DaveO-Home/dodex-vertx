package dmo.fs.vertx;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.ThreadingModel;

public class DodexLauncher extends io.vertx.core.Launcher {
  @Override
  public void beforeDeployingVerticle(DeploymentOptions deploymentOptions) {
    deploymentOptions.setThreadingModel(ThreadingModel.VIRTUAL_THREAD);
  }
}
