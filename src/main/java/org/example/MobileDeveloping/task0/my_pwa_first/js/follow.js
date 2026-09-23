class FollowController {
  static STORAGE_KEY_FOLLOWING = 'isFollowing';
  static STORAGE_KEY_FOLLOWERS = 'followersCount';
  static DEFAULT_FOLLOWERS = 2;

  constructor(selectors) {
    this.button = document.querySelector(selectors.button);
    this.followersValue = document.querySelector(selectors.followersValue);
  }

  isFollowing() {
    return localStorage.getItem(FollowController.STORAGE_KEY_FOLLOWING) === 'true';
  }

  setFollowing(value) {
    localStorage.setItem(FollowController.STORAGE_KEY_FOLLOWING, String(value));
  }

  getFollowersCount() {
    const saved = localStorage.getItem(FollowController.STORAGE_KEY_FOLLOWERS);
    if (saved === null) {
      return FollowController.DEFAULT_FOLLOWERS;
    }
    const parsed = Number.parseInt(saved, 10);
    return Number.isNaN(parsed) ? FollowController.DEFAULT_FOLLOWERS : parsed;
  }

  setFollowersCount(count) {
    localStorage.setItem(FollowController.STORAGE_KEY_FOLLOWERS, String(count));
  }

  render() {
    if (!this.button || !this.followersValue) return;

    const following = this.isFollowing();
    const count = this.getFollowersCount();

    this.button.textContent = following ? 'Отписаться' : 'Подписаться';
    this.button.classList.toggle('profile-button--following', following);
    this.followersValue.textContent = String(count);
  }

  toggle() {
    if (!this.button || !this.followersValue) return;

    const currentlyFollowing = this.isFollowing();
    let count = this.getFollowersCount();

    if (!currentlyFollowing) {
      this.setFollowing(true);
      count += 1;
    } else {
      this.setFollowing(false);
      count = Math.max(0, count - 1);
    }

    this.setFollowersCount(count);
    this.render();
  }
}

const followController = new FollowController({
  button: '.profile-button',
  followersValue: '[data-stat="followers"]',
});

function toggleFollow() {
  followController.toggle();
}

(function initFollow() {
  followController.render();
})();
