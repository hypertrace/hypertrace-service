{{- define "hypertraceservice.image" -}}
  {{- if and .Values.image.tagOverride  -}}
    {{- printf "%s:%s" .Values.image.repository .Values.image.tagOverride }}
  {{- else -}}
    {{- printf "%s:%s" .Values.image.repository .Chart.AppVersion }}
  {{- end -}}
{{- end -}}

{{- /* Refer https://github.com/openstack/openstack-helm-infra/blob/master/helm-toolkit/templates/utils/_joinListWithComma.tpl */}}
{{- define "helm-toolkit.utils.joinListWithComma" -}}
{{- $local := dict "first" true -}}
{{- range $k, $v := . -}}{{- if not $local.first -}},{{- end -}}{{- $v -}}{{- $_ := set $local "first" false -}}{{- end -}}
{{- end -}}