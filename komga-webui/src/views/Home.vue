<template>
  <div class="fill-height">
    <v-app-bar
      app
    >
      <v-badge
        dot
        offset-x="15"
        offset-y="20"
        :value="drawerVisible ? 0 : booksToCheck"
        color="accent"
        :style="`margin-${$vuetify.rtl ? 'right' : 'left' }: -12px`"
      >
        <v-app-bar-nav-icon @click.stop="toggleDrawer"/>
      </v-badge>

      <search-box class="flex-fill"/>

    </v-app-bar>

    <v-navigation-drawer app v-model="drawerVisible" :right="$vuetify.rtl">
      <v-list-item @click="$router.push({name: 'home'})" inactive class="pb-2">
        <v-list-item-avatar>
          <v-img src="../assets/logo.svg"/>
        </v-list-item-avatar>

        <v-list-item-content>
          <v-list-item-title class="title">
            Komga
          </v-list-item-title>
        </v-list-item-content>

        <v-tooltip left>
          <template v-slot:activator="{ on }">
            <v-progress-linear
              :active="taskCount > 0"
              indeterminate
              absolute
              bottom
              height="2"
              color="secondary"
              v-on="on"
            />
          </template>
          <span>{{ $tc('common.pending_tasks', taskCount) }}</span>
        </v-tooltip>
      </v-list-item>

      <v-divider/>

      <v-list>
        <v-list-item :to="{name: 'dashboard'}">
          <v-list-item-icon>
            <v-icon>mdi-home</v-icon>
          </v-list-item-icon>
          <v-list-item-content>
            <v-list-item-title>{{ $t('navigation.home') }}</v-list-item-title>
          </v-list-item-content>
        </v-list-item>

        <v-list-item :to="{name:'libraries', params: {libraryId: LIBRARIES_ALL}}">
          <v-list-item-icon>
            <v-icon>mdi-book-multiple</v-icon>
          </v-list-item-icon>
          <v-list-item-content>
            <v-list-item-title>{{ $t('navigation.libraries') }}</v-list-item-title>
          </v-list-item-content>
          <v-list-item-action v-if="isAdmin">
            <v-btn icon @click.stop.capture.prevent="addLibrary">
              <v-icon>mdi-plus</v-icon>
            </v-btn>
          </v-list-item-action>
        </v-list-item>

        <v-list-item v-for="(l, index) in libraries"
                     :key="index"
                     dense
                     :to="{name:'libraries', params: {libraryId: l.id}}"
        >
          <v-list-item-icon>
          </v-list-item-icon>
          <v-list-item-content>
            <v-list-item-title>{{ l.name }}</v-list-item-title>
          </v-list-item-content>
          <v-list-item-action v-if="isAdmin">
            <library-actions-menu :library="l"/>
          </v-list-item-action>
        </v-list-item>

        <v-list-item :to="{name: 'import'}" v-if="isAdmin">
          <v-list-item-icon>
            <v-icon>mdi-import</v-icon>
          </v-list-item-icon>
          <v-list-item-content>
            <v-list-item-title>{{ $t('book_import.title') }}</v-list-item-title>
          </v-list-item-content>
        </v-list-item>

        <v-list-item :to="{name: 'settings'}" v-if="isAdmin">
          <v-list-item-action>
            <v-icon>mdi-cog</v-icon>
          </v-list-item-action>
          <v-list-item-content>
            <v-badge
              dot
              inline
              :value="booksToCheck"
              color="accent"
            >
              <v-list-item-title>{{ $t('server_settings.server_settings') }}</v-list-item-title>
            </v-badge>
          </v-list-item-content>
        </v-list-item>

        <v-list-item :to="{name: 'account'}">
          <v-list-item-action>
            <v-icon>mdi-account</v-icon>
          </v-list-item-action>
          <v-list-item-content>
            <v-list-item-title>{{ $t('account_settings.account_settings') }}</v-list-item-title>
          </v-list-item-content>
        </v-list-item>

        <v-list-item @click="logout">
          <v-list-item-icon>
            <v-icon>mdi-power</v-icon>
          </v-list-item-icon>
          <v-list-item-content>
            <v-list-item-title>{{ $t('navigation.logout') }}</v-list-item-title>
          </v-list-item-content>
        </v-list-item>
      </v-list>

      <v-divider/>

      <v-list>
        <v-list-item>
          <v-list-item-icon>
            <v-icon>{{ themeIcon }}</v-icon>
          </v-list-item-icon>
          <v-select
            v-model="theme"
            :items="themes"
            :label="$t('home.theme')"
          ></v-select>
        </v-list-item>
      </v-list>

      <v-list>
        <v-list-item>
          <v-list-item-icon>
            <v-icon>mdi-translate</v-icon>
          </v-list-item-icon>
          <v-select v-model="locale"
                    :items="locales"
                    :label="$t('home.translation')"
          >
          </v-select>
        </v-list-item>
      </v-list>

      <v-spacer/>

      <template v-slot:append>
        <div v-if="isAdmin && !$_.isEmpty(info)"
             class="pa-2 pb-6 text-caption"
        >
          <div>v{{ info.build.version }}-{{ info.git.branch }}</div>
        </div>
      </template>
    </v-navigation-drawer>

    <v-main class="fill-height">
      <dialogs/>
      <toaster/>
      <router-view/>
    </v-main>
  </div>
</template>

<script lang="ts">
import Dialogs from '@/components/Dialogs.vue'
import LibraryActionsMenu from '@/components/menus/LibraryActionsMenu.vue'
import SearchBox from '@/components/SearchBox.vue'
import {Theme} from '@/types/themes'
import Vue from 'vue'
import {LIBRARIES_ALL} from "@/types/library"
import Toaster from "@/components/Toaster.vue"
import {MediaStatus} from "@/types/enum-books";

export default Vue.extend({
  name: 'home',
  components: {Toaster, LibraryActionsMenu, SearchBox, Dialogs},
  data: function () {
    return {
      LIBRARIES_ALL,
      drawerVisible: this.$vuetify.breakpoint.lgAndUp,
      info: {} as ActuatorInfo,
      locales: this.$i18n.availableLocales.map((x: any) => ({text: this.$i18n.t('common.locale_name', x), value: x})),
    }
  },
  async created() {
    if (this.isAdmin) {
      this.info = await this.$actuator.getInfo()
      this.$komgaBooks.getBooks(undefined, {size: 0} as PageRequest, undefined, [MediaStatus.ERROR, MediaStatus.UNSUPPORTED])
        .then(x => this.$store.commit('setBooksToCheck', x.totalElements))
    }
  },
  computed: {
    booksToCheck(): number {
      return this.$store.state.booksToCheck
    },
    taskCount(): number {
      return this.$store.state.komgaSse.taskCount
    },
    libraries(): LibraryDto[] {
      return this.$store.state.komgaLibraries.libraries
    },
    isAdmin(): boolean {
      return this.$store.getters.meAdmin
    },
    themes(): object[] {
      return [
        {text: this.$i18n.t(Theme.LIGHT), value: Theme.LIGHT},
        {text: this.$i18n.t(Theme.DARK), value: Theme.DARK},
        {text: this.$i18n.t(Theme.SYSTEM), value: Theme.SYSTEM},
      ]
    },
    themeIcon(): string {
      switch (this.theme) {
        case Theme.LIGHT:
          return 'mdi-brightness-7'
        case Theme.DARK:
          return 'mdi-brightness-3'
        case Theme.SYSTEM:
          return 'mdi-brightness-auto'
      }
      return ''
    },

    theme: {
      get: function (): Theme {
        return this.$store.state.persistedState.theme
      },
      set: function (theme: Theme): void {
        if (Object.values(Theme).includes(theme)) {
          this.$store.commit('setTheme', theme)
        }
      },
    },
    locale: {
      get: function (): string {
        return this.$i18n.locale
      },
      set: function (locale: string): void {
        if (this.$i18n.availableLocales.includes(locale)) {
          this.$store.commit('setLocale', locale)
        }
      },
    },
  },
  methods: {
    toggleDrawer() {
      this.drawerVisible = !this.drawerVisible
    },
    logout() {
      this.$store.dispatch('logout')
      this.$router.push({name: 'login'})
    },
    addLibrary() {
      this.$store.dispatch('dialogAddLibrary')
    },
  },
})
</script>
