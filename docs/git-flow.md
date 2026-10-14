# Git Flow 分支說明

![Git Flow 分支流程圖](./flow.jpg)

| 分支 | 用途 | 從哪裡建立 | 完成後合併到 |
| --- | --- | --- | --- |
| `master` | 保存可上線的穩定版本，不直接在這裡開發。 | — | — |
| `develop` | 集中整合新功能，準備下一版。 | `master` | 透過 `release` 整理後發布 |
| `feature` | 開發個別新功能。 | `develop` | `develop` |
| `release` | 上線前測試與修正。 | `develop` | `master` 和 `develop` |
| `hotfix` | 緊急修復線上版本的問題。 | `master` | `master` 和 `develop` |

`master` 和 `develop` 長期保留，其他分支完成並合併後通常會刪除。

`release` 與 `hotfix` 的修正也要合併回 `develop`，讓後續版本包含相同修正。

參考來源：[為你自己學 Git：Git Flow 是什麼？為什麼需要這種東西？](https://gitbook.tw/chapters/gitflow/why-need-git-flow)
