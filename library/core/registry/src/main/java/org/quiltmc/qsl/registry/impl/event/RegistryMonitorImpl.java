/*
 * Copyright 2022 QuiltMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.quiltmc.qsl.registry.impl.event;

import java.util.Map;
import java.util.function.Predicate;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Holder;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import org.quiltmc.qsl.registry.api.event.RegistryEntryContext;
import org.quiltmc.qsl.registry.api.event.RegistryEvents;
import org.quiltmc.qsl.registry.api.event.RegistryMonitor;
import org.quiltmc.qsl.registry.mixin.SimpleRegistryAccessor;

/**
 * The default implementation of {@link RegistryMonitor}.
 *
 * @param <V> the entry type of the monitored {@link Registry}
 */
@ApiStatus.Internal
public class RegistryMonitorImpl<V> implements RegistryMonitor<V> {
	private final Registry<V> registry;
	private @Nullable Predicate<RegistryEntryContext<V>> filter = null;

	public RegistryMonitorImpl(Registry<V> registry) {
		this.registry = registry;
	}

	@Override
	public RegistryMonitor<V> filter(Predicate<RegistryEntryContext<V>> filter) {
		this.filter = this.filter == null ? filter : this.filter.and(filter);
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void forAll(RegistryEvents.EntryAdded<V> callback) {
		var context = new MutableRegistryEntryContextImpl<>(this.registry);

		if (!(this.registry instanceof SimpleRegistryAccessor)) {
			throw new UnsupportedOperationException("Registry " + this.registry + " is not supported!");
		}

		for (Map.Entry<Identifier, Holder.Reference<V>> entry : ((SimpleRegistryAccessor<V>) this.registry).getIdToEntryMap().entrySet()) {
			context.set(entry.getKey(), entry.getValue().value());

			if (this.testFilter(context)) {
				callback.onAdded(context);
			}
		}

		this.forUpcoming(callback);
	}

	@Override
	public void forUpcoming(RegistryEvents.EntryAdded<V> callback) {
		RegistryEvents.getEntryAddEvent(this.registry).register(context -> {
			if (this.testFilter(context)) {
				callback.onAdded(context);
			}
		});
	}

	/**
	 * Tests the current filter on the specified entry context.
	 * <p>
	 * Accounts for the filter being {@code null} by treating it as always {@code true}.
	 */
	private boolean testFilter(RegistryEntryContext<V> context) {
		if (this.filter == null) {
			return true;
		}

		return this.filter.test(context);
	}
}