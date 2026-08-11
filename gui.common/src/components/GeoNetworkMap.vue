<template>
  <section class="common-geo-map" :class="{ 'common-geo-map--fullscreen': fullscreen }">
    <div class="common-geo-map__toolbar">
      <strong>{{ title }}</strong>
      <span>{{ summary }}</span>
      <button type="button" title="Zoom in" @click="zoomBy(1)">+</button>
      <button type="button" title="Zoom out" @click="zoomBy(-1)">-</button>
      <button type="button" title="Fit to grid" @click="fitToBounds">Fit</button>
      <button type="button" :title="fullscreen ? 'Minimize map' : 'Maximize map'" @click="fullscreen = !fullscreen">
        {{ fullscreen ? 'Minimize' : 'Maximize' }}
      </button>
    </div>

    <div
      ref="mapElement"
      class="common-geo-map__viewport"
      @pointerdown="startPan"
      @pointermove="pan"
      @pointerup="endPan"
      @pointercancel="endPan"
      @pointerleave="endPan"
      @wheel.prevent="wheelZoom"
    >
      <img
        v-for="tile in visibleTiles"
        :key="tile.key"
        class="common-geo-map__tile"
        :src="tile.src"
        :style="{ transform: `translate(${tile.left}px, ${tile.top}px)` }"
        alt=""
        draggable="false"
      />

      <svg class="common-geo-map__overlay" :viewBox="`0 0 ${viewport.width} ${viewport.height}`">
        <polyline
          v-for="line in projectedLines"
          :key="line.id"
          class="common-geo-map__line"
          :points="line.points"
        >
          <title>{{ line.label }}</title>
        </polyline>
      </svg>

      <button
        v-for="cluster in clusters"
        :key="cluster.id"
        type="button"
        class="common-geo-map__marker"
        :class="{ 'common-geo-map__marker--cluster': cluster.count > 1 }"
        :style="{ transform: `translate(${cluster.x}px, ${cluster.y}px)` }"
        @click.stop="selectCluster(cluster)"
      >
        {{ cluster.count > 1 ? cluster.count : '' }}
      </button>

      <article
        v-if="selectedPoint"
        class="common-geo-map__popup"
        :style="{ transform: `translate(${selectedPoint.x}px, ${selectedPoint.y}px)` }"
      >
        <button type="button" title="Close" @click="selectedCluster = undefined">x</button>
        <strong>{{ selectedPoint.label }}</strong>
        <span>{{ selectedPoint.latitude.toFixed(6) }}, {{ selectedPoint.longitude.toFixed(6) }}</span>
        <dl>
          <template v-for="entry in selectedDetails" :key="entry.key">
            <dt>{{ entry.key }}</dt>
            <dd>{{ entry.value }}</dd>
          </template>
        </dl>
      </article>

      <p v-if="points.length === 0 && lines.length === 0" class="common-geo-map__empty">No GL coordinates available</p>
    </div>

    <p class="common-geo-map__attribution">
      {{ attribution }}
    </p>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue';

export interface GeoMapBounds {
  minLatitude: number;
  maxLatitude: number;
  minLongitude: number;
  maxLongitude: number;
}

export interface GeoMapPoint {
  id: string;
  label: string;
  latitude: number;
  longitude: number;
  details?: Record<string, unknown>;
}

export interface GeoMapLine {
  id: string;
  label: string;
  points: GeoMapPoint[];
  details?: Record<string, unknown>;
}

const props = withDefaults(defineProps<{
  title?: string;
  points: GeoMapPoint[];
  lines?: GeoMapLine[];
  bounds?: GeoMapBounds;
  tileUrl?: string;
  attribution?: string;
}>(), {
  title: 'Grid map',
  lines: () => [],
  tileUrl: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
  attribution: 'Map data © OpenStreetMap contributors'
});

const tileSize = 256;
const minZoom = 2;
const maxZoom = 15;
const mapElement = ref<HTMLElement>();
const viewport = ref({ width: 960, height: 520 });
const centerLatitude = ref(48);
const centerLongitude = ref(9);
const zoom = ref(6);
const fullscreen = ref(false);
const selectedCluster = ref<ProjectedCluster>();
let resizeObserver: ResizeObserver | undefined;
let drag: { x: number; y: number; centerX: number; centerY: number } | undefined;

const summary = computed(() => `${props.points.length} points, ${props.lines.length} lines, zoom ${zoom.value}`);
const centerWorld = computed(() => project(centerLatitude.value, centerLongitude.value, zoom.value));
const origin = computed(() => ({
  x: centerWorld.value.x - viewport.value.width / 2,
  y: centerWorld.value.y - viewport.value.height / 2
}));

const visibleTiles = computed(() => {
  const firstX = Math.floor(origin.value.x / tileSize) - 1;
  const lastX = Math.floor((origin.value.x + viewport.value.width) / tileSize) + 1;
  const firstY = Math.max(0, Math.floor(origin.value.y / tileSize) - 1);
  const lastY = Math.min(Math.pow(2, zoom.value) - 1, Math.floor((origin.value.y + viewport.value.height) / tileSize) + 1);
  const tiles: Array<{ key: string; src: string; left: number; top: number }> = [];
  const tileCount = Math.pow(2, zoom.value);
  for (let x = firstX; x <= lastX; x += 1) {
    const wrappedX = ((x % tileCount) + tileCount) % tileCount;
    for (let y = firstY; y <= lastY; y += 1) {
      tiles.push({
        key: `${zoom.value}-${wrappedX}-${y}-${x}`,
        src: props.tileUrl
          .replace('{z}', String(zoom.value))
          .replace('{x}', String(wrappedX))
          .replace('{y}', String(y)),
        left: x * tileSize - origin.value.x,
        top: y * tileSize - origin.value.y
      });
    }
  }
  return tiles;
});

const projectedPoints = computed(() => props.points
  .filter((point) => Number.isFinite(point.latitude) && Number.isFinite(point.longitude))
  .map((point) => {
    const position = project(point.latitude, point.longitude, zoom.value);
    return {
      ...point,
      x: position.x - origin.value.x,
      y: position.y - origin.value.y
    };
  }));

const projectedLines = computed(() => props.lines
  .map((line) => ({
    id: line.id,
    label: line.label,
    points: line.points
      .filter((point) => Number.isFinite(point.latitude) && Number.isFinite(point.longitude))
      .map((point) => {
        const position = project(point.latitude, point.longitude, zoom.value);
        return `${position.x - origin.value.x},${position.y - origin.value.y}`;
      })
      .join(' ')
  }))
  .filter((line) => line.points.length > 0));

const clusters = computed<ProjectedCluster[]>(() => {
  const grid = new Map<string, ProjectedPoint[]>();
  for (const point of projectedPoints.value) {
    const key = `${Math.round(point.x / 34)}:${Math.round(point.y / 34)}`;
    const values = grid.get(key) ?? [];
    values.push(point);
    grid.set(key, values);
  }
  return Array.from(grid.entries()).map(([key, values]) => ({
    id: key,
    x: values.reduce((sum, point) => sum + point.x, 0) / values.length,
    y: values.reduce((sum, point) => sum + point.y, 0) / values.length,
    count: values.length,
    points: values
  }));
});

const selectedPoint = computed(() => {
  const cluster = selectedCluster.value;
  if (!cluster || cluster.points.length !== 1) {
    return undefined;
  }
  return cluster.points[0];
});

const selectedDetails = computed(() => Object.entries(selectedPoint.value?.details ?? {})
  .filter(([, value]) => value !== undefined && value !== null && String(value).length > 0)
  .slice(0, 12)
  .map(([key, value]) => ({ key, value: String(value) })));

watch(() => props.bounds, () => nextTick(fitToBounds), { immediate: true });
watch(fullscreen, () => nextTick(() => {
  measure();
  fitToBounds();
}));

onMounted(() => {
  measure();
  resizeObserver = new ResizeObserver(measure);
  if (mapElement.value) {
    resizeObserver.observe(mapElement.value);
  }
  fitToBounds();
});

onUnmounted(() => {
  resizeObserver?.disconnect();
});

function measure() {
  if (!mapElement.value) {
    return;
  }
  viewport.value = {
    width: Math.max(mapElement.value.clientWidth, 320),
    height: Math.max(mapElement.value.clientHeight, 320)
  };
}

function fitToBounds() {
  const bounds = props.bounds ?? boundsFromPoints(props.points);
  if (!bounds) {
    centerLatitude.value = 48;
    centerLongitude.value = 9;
    zoom.value = 4;
    return;
  }
  centerLatitude.value = (bounds.minLatitude + bounds.maxLatitude) / 2;
  centerLongitude.value = (bounds.minLongitude + bounds.maxLongitude) / 2;
  for (let candidateZoom = maxZoom; candidateZoom >= minZoom; candidateZoom -= 1) {
    const northWest = project(bounds.maxLatitude, bounds.minLongitude, candidateZoom);
    const southEast = project(bounds.minLatitude, bounds.maxLongitude, candidateZoom);
    if (Math.abs(southEast.x - northWest.x) <= viewport.value.width * 0.82
        && Math.abs(southEast.y - northWest.y) <= viewport.value.height * 0.78) {
      zoom.value = candidateZoom;
      return;
    }
  }
  zoom.value = minZoom;
}

function zoomBy(delta: number) {
  zoom.value = clampZoom(zoom.value + delta);
}

function wheelZoom(event: WheelEvent) {
  zoomBy(event.deltaY < 0 ? 1 : -1);
}

function startPan(event: PointerEvent) {
  if (!mapElement.value) {
    return;
  }
  mapElement.value.setPointerCapture(event.pointerId);
  drag = { x: event.clientX, y: event.clientY, centerX: centerWorld.value.x, centerY: centerWorld.value.y };
}

function pan(event: PointerEvent) {
  if (!drag) {
    return;
  }
  const worldX = drag.centerX - (event.clientX - drag.x);
  const worldY = drag.centerY - (event.clientY - drag.y);
  const next = unproject(worldX, worldY, zoom.value);
  centerLatitude.value = next.latitude;
  centerLongitude.value = next.longitude;
}

function endPan() {
  drag = undefined;
}

function selectCluster(cluster: ProjectedCluster) {
  if (cluster.count > 1) {
    const first = cluster.points[0];
    centerLatitude.value = first.latitude;
    centerLongitude.value = first.longitude;
    zoom.value = clampZoom(zoom.value + 2);
    selectedCluster.value = undefined;
    return;
  }
  selectedCluster.value = cluster;
}

function project(latitude: number, longitude: number, zoomLevel: number) {
  const sinLatitude = Math.sin((Math.max(-85.05112878, Math.min(85.05112878, latitude)) * Math.PI) / 180);
  const scale = tileSize * Math.pow(2, zoomLevel);
  return {
    x: ((longitude + 180) / 360) * scale,
    y: (0.5 - Math.log((1 + sinLatitude) / (1 - sinLatitude)) / (4 * Math.PI)) * scale
  };
}

function unproject(x: number, y: number, zoomLevel: number) {
  const scale = tileSize * Math.pow(2, zoomLevel);
  const longitude = (x / scale) * 360 - 180;
  const mercator = Math.PI - (2 * Math.PI * y) / scale;
  const latitude = (180 / Math.PI) * Math.atan(0.5 * (Math.exp(mercator) - Math.exp(-mercator)));
  return { latitude, longitude };
}

function clampZoom(value: number) {
  return Math.max(minZoom, Math.min(maxZoom, value));
}

function boundsFromPoints(points: GeoMapPoint[]): GeoMapBounds | undefined {
  const validPoints = points.filter((point) => Number.isFinite(point.latitude) && Number.isFinite(point.longitude));
  if (validPoints.length === 0) {
    return undefined;
  }
  return {
    minLatitude: Math.min(...validPoints.map((point) => point.latitude)),
    maxLatitude: Math.max(...validPoints.map((point) => point.latitude)),
    minLongitude: Math.min(...validPoints.map((point) => point.longitude)),
    maxLongitude: Math.max(...validPoints.map((point) => point.longitude))
  };
}

type ProjectedPoint = GeoMapPoint & { x: number; y: number };

interface ProjectedCluster {
  id: string;
  x: number;
  y: number;
  count: number;
  points: ProjectedPoint[];
}
</script>
