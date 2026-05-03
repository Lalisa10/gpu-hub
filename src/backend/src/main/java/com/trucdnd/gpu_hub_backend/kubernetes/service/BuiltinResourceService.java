package com.trucdnd.gpu_hub_backend.kubernetes.service;

import java.io.OutputStream;
import java.util.List;

import org.springframework.stereotype.Service;

import com.trucdnd.gpu_hub_backend.cluster.entity.Cluster;
import com.trucdnd.gpu_hub_backend.common.exception.KubernetesOperationException;
import com.trucdnd.gpu_hub_backend.kubernetes.factory.KubernetesClientFactory;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.PersistentVolume;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class BuiltinResourceService {

    private final KubernetesClientFactory clientFactory;

    // ── Namespace ─────────────────────────────────────────────────────────────

    public Namespace createNamespace(Cluster cluster, String nsName) {
        try {
            KubernetesClient client = clientFactory.createClient(cluster);
            return client.namespaces().resource(
                    new NamespaceBuilder()
                            .withNewMetadata().withName(nsName).endMetadata()
                            .build())
                    .create();
        } catch (KubernetesClientException e) {
            throw k8sError("create namespace '" + nsName + "'", cluster, e);
        }
    }

    public void deleteNamespace(Cluster cluster, String nsName) {
        try {
            KubernetesClient client = clientFactory.createClient(cluster);
            client.namespaces().withName(nsName).delete();
        } catch (KubernetesClientException e) {
            throw k8sError("delete namespace '" + nsName + "'", cluster, e);
        }
    }

    public List<Namespace> getNamespaces(Cluster cluster) {
        try {
            KubernetesClient client = clientFactory.createClient(cluster);
            return client.namespaces().list().getItems();
        } catch (KubernetesClientException e) {
            throw k8sError("list namespaces", cluster, e);
        }
    }

    // ── Deployment ────────────────────────────────────────────────────────────

    public Deployment createDeployment(Cluster cluster, String namespace, Deployment deployment) {
        try {
            KubernetesClient client = clientFactory.createClient(cluster);
            return client.apps().deployments().inNamespace(namespace).resource(deployment).create();
        } catch (KubernetesClientException e) {
            throw k8sError("create deployment in namespace '" + namespace + "'", cluster, e);
        }
    }

    public Deployment updateDeployment(Cluster cluster, String namespace, Deployment deployment) {
        try {
            KubernetesClient client = clientFactory.createClient(cluster);
            return client.apps().deployments().inNamespace(namespace).resource(deployment).update();
        } catch (KubernetesClientException e) {
            throw k8sError("update deployment in namespace '" + namespace + "'", cluster, e);
        }
    }

    public void deleteDeployment(Cluster cluster, String namespace, String deploymentName) {
        try {
            KubernetesClient client = clientFactory.createClient(cluster);
            client.apps().deployments().inNamespace(namespace).withName(deploymentName).delete();
        } catch (KubernetesClientException e) {
            if (e.getCode() == 404) return;
            throw k8sError("delete deployment '" + deploymentName + "' in namespace '" + namespace + "'", cluster, e);
        }
    }

    public void deleteDeploymentsByLabel(Cluster cluster, String namespace, String labelKey, String labelValue) {
        try {
            KubernetesClient client = clientFactory.createClient(cluster);
            client.apps().deployments().inNamespace(namespace).withLabel(labelKey, labelValue).delete();
        } catch (KubernetesClientException e) {
            if (e.getCode() == 404) return;
            throw k8sError("delete deployments by label " + labelKey + "=" + labelValue
                    + " in namespace '" + namespace + "'", cluster, e);
        }
    }

    // ── Pods ──────────────────────────────────────────────────────────────────

    public List<Pod> listPodsByLabel(Cluster cluster, String namespace, Map<String, String> labels) {
        try {
            KubernetesClient client = clientFactory.createClient(cluster);
            return client.pods().inNamespace(namespace).withLabels(labels).list().getItems();
        } catch (KubernetesClientException e) {
            throw k8sError("list pods by labels " + labels + " in namespace '" + namespace + "'", cluster, e);
        }
    }

    // ── Pod logs ──────────────────────────────────────────────────────────────

    public String getPodLogs(Cluster cluster, String namespace, String podName) {
        try {
            KubernetesClient client = clientFactory.createClient(cluster);
            return client.pods().inNamespace(namespace).withName(podName).getLog();
        } catch (KubernetesClientException e) {
            throw k8sError("get pod logs for '" + podName + "' in namespace '" + namespace + "'", cluster, e);
        }
    }

    public String getPodLogs(Cluster cluster, String namespace, String podName, String containerName) {
        try {
            KubernetesClient client = clientFactory.createClient(cluster);
            return client.pods().inNamespace(namespace).withName(podName)
                    .inContainer(containerName).getLog();
        } catch (KubernetesClientException e) {
            throw k8sError("get pod logs for '" + podName + "/" + containerName + "' in namespace '" + namespace + "'", cluster, e);
        }
    }

    public LogWatch watchPodLogs(Cluster cluster, String namespace, String podName, OutputStream out) {
        try {
            KubernetesClient client = clientFactory.createClient(cluster);
            return client.pods().inNamespace(namespace).withName(podName).watchLog(out);
        } catch (KubernetesClientException e) {
            throw k8sError("watch pod logs for '" + podName + "' in namespace '" + namespace + "'", cluster, e);
        }
    }

    public LogWatch watchPodLogs(Cluster cluster, String namespace, String podName,
            String containerName, OutputStream out) {
        try {
            KubernetesClient client = clientFactory.createClient(cluster);
            return client.pods().inNamespace(namespace).withName(podName)
                    .inContainer(containerName).watchLog(out);
        } catch (KubernetesClientException e) {
            throw k8sError("watch pod logs for '" + podName + "/" + containerName + "' in namespace '" + namespace + "'", cluster, e);
        }
    }

    // ── Secrets ───────────────────────────────────────────────────────────────

    public Secret createSecret(Cluster cluster, String namespace, Secret secret) {
        try {
            KubernetesClient client = clientFactory.createClient(cluster);
            return client.secrets().inNamespace(namespace).resource(secret).create();
        } catch (KubernetesClientException e) {
            throw k8sError("create secret in namespace '" + namespace + "'", cluster, e);
        }
    }

    public void deleteSecretsByLabel(Cluster cluster, String namespace, String labelKey, String labelValue) {
        try {
            KubernetesClient client = clientFactory.createClient(cluster);
            client.secrets().inNamespace(namespace).withLabel(labelKey, labelValue).delete();
        } catch (KubernetesClientException e) {
            if (e.getCode() == 404) return;
            throw k8sError("delete secrets by label " + labelKey + "=" + labelValue
                    + " in namespace '" + namespace + "'", cluster, e);
        }
    }

    // ── PersistentVolumes (cluster-scoped) ────────────────────────────────────

    public PersistentVolume createPersistentVolume(Cluster cluster, PersistentVolume pv) {
        try {
            KubernetesClient client = clientFactory.createClient(cluster);
            return client.persistentVolumes().resource(pv).create();
        } catch (KubernetesClientException e) {
            throw k8sError("create persistent volume", cluster, e);
        }
    }

    public void deletePersistentVolumesByLabel(Cluster cluster, String labelKey, String labelValue) {
        try {
            KubernetesClient client = clientFactory.createClient(cluster);
            client.persistentVolumes().withLabel(labelKey, labelValue).delete();
        } catch (KubernetesClientException e) {
            if (e.getCode() == 404) return;
            throw k8sError("delete persistent volumes by label " + labelKey + "=" + labelValue, cluster, e);
        }
    }

    // ── PersistentVolumeClaims ────────────────────────────────────────────────

    public PersistentVolumeClaim createPersistentVolumeClaim(Cluster cluster, String namespace,
            PersistentVolumeClaim pvc) {
        try {
            KubernetesClient client = clientFactory.createClient(cluster);
            return client.persistentVolumeClaims().inNamespace(namespace).resource(pvc).create();
        } catch (KubernetesClientException e) {
            throw k8sError("create persistent volume claim in namespace '" + namespace + "'", cluster, e);
        }
    }

    public boolean persistentVolumeClaimExists(Cluster cluster, String namespace, String name) {
        try {
            KubernetesClient client = clientFactory.createClient(cluster);
            return client.persistentVolumeClaims().inNamespace(namespace).withName(name).get() != null;
        } catch (KubernetesClientException e) {
            throw k8sError("check existence of pvc '" + name + "' in namespace '" + namespace + "'", cluster, e);
        }
    }

    public void deletePersistentVolumeClaimsByLabel(Cluster cluster, String namespace,
            String labelKey, String labelValue) {
        try {
            KubernetesClient client = clientFactory.createClient(cluster);
            client.persistentVolumeClaims().inNamespace(namespace).withLabel(labelKey, labelValue).delete();
        } catch (KubernetesClientException e) {
            if (e.getCode() == 404) return;
            throw k8sError("delete persistent volume claims by label " + labelKey + "=" + labelValue
                    + " in namespace '" + namespace + "'", cluster, e);
        }
    }

    // ── Jobs ──────────────────────────────────────────────────────────────────

    public Job createJob(Cluster cluster, String namespace, Job job) {
        try {
            KubernetesClient client = clientFactory.createClient(cluster);
            return client.batch().v1().jobs().inNamespace(namespace).resource(job).create();
        } catch (KubernetesClientException e) {
            throw k8sError("create job in namespace '" + namespace + "'", cluster, e);
        }
    }

    public Job getJob(Cluster cluster, String namespace, String name) {
        try {
            KubernetesClient client = clientFactory.createClient(cluster);
            return client.batch().v1().jobs().inNamespace(namespace).withName(name).get();
        } catch (KubernetesClientException e) {
            throw k8sError("get job '" + name + "' in namespace '" + namespace + "'", cluster, e);
        }
    }

    public void deleteJobsByLabel(Cluster cluster, String namespace, String labelKey, String labelValue) {
        try {
            KubernetesClient client = clientFactory.createClient(cluster);
            client.batch().v1().jobs().inNamespace(namespace).withLabel(labelKey, labelValue).delete();
        } catch (KubernetesClientException e) {
            if (e.getCode() == 404) return;
            throw k8sError("delete jobs by label " + labelKey + "=" + labelValue
                    + " in namespace '" + namespace + "'", cluster, e);
        }
    }

    // ── Nodes ─────────────────────────────────────────────────────────────────

    public List<Node> listNodes(Cluster cluster) {
        try {
            return clientFactory.createClient(cluster).nodes().list().getItems();
        } catch (KubernetesClientException e) {
            throw k8sError("list nodes", cluster, e);
        }
    }

    private KubernetesOperationException k8sError(String action, Cluster cluster, KubernetesClientException e) {
        String clusterName = cluster != null ? cluster.getName() : "unknown";
        String detail = e.getMessage() != null ? e.getMessage() : "Unknown Kubernetes error";
        return new KubernetesOperationException(
                "Kubernetes operation failed while trying to " + action + " on cluster '" + clusterName + "': " + detail,
                e
        );
    }
}
