package com.distribuidora.agua.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "produtos")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String sku;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(name = "capacidade_litros", nullable = false, precision = 5, scale = 2)
    private BigDecimal capacidadeLitros;

    @Column(nullable = false)
    private boolean retornavel;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_produto", nullable = false, length = 30)
    private TipoProduto tipoProduto;

    @Column(name = "preco_venda", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoVenda;

    @Column(name = "estoque_cheios", nullable = false)
    private int estoqueCheios;

    @Column(name = "estoque_vazios", nullable = false)
    private int estoqueVazios;

    @Column(nullable = false)
    private boolean ativo = true;

    @Version
    @Column(nullable = false)
    private Long versao;

    protected Product() {
    }

    public Product(Long id, String sku, String nome, BigDecimal capacidadeLitros, boolean retornavel,
                   TipoProduto tipoProduto, BigDecimal precoVenda, int estoqueCheios, int estoqueVazios,
                   boolean ativo, Long versao) {
        this.id = id;
        this.sku = Objects.requireNonNull(sku, "SKU nao pode ser nulo");
        this.nome = Objects.requireNonNull(nome, "Nome nao pode ser nulo");
        this.capacidadeLitros = Objects.requireNonNull(capacidadeLitros, "Capacidade em litros nao pode ser nula");
        this.retornavel = retornavel;
        this.tipoProduto = Objects.requireNonNull(tipoProduto, "Tipo do produto nao pode ser nulo");
        this.precoVenda = Objects.requireNonNull(precoVenda, "Preco de venda nao pode ser nulo");
        this.estoqueCheios = estoqueCheios;
        this.estoqueVazios = estoqueVazios;
        this.ativo = ativo;
        this.versao = versao;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getNome() {
        return nome;
    }

    public BigDecimal getCapacidadeLitros() {
        return capacidadeLitros;
    }

    public boolean isRetornavel() {
        return retornavel;
    }

    public TipoProduto getTipoProduto() {
        return tipoProduto;
    }

    public BigDecimal getPrecoVenda() {
        return precoVenda;
    }

    public int getEstoqueCheios() {
        return estoqueCheios;
    }

    public int getEstoqueVazios() {
        return estoqueVazios;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public Long getVersao() {
        return versao;
    }

    public void setEstoqueCheios(int estoqueCheios) {
        this.estoqueCheios = estoqueCheios;
    }

    public void setEstoqueVazios(int estoqueVazios) {
        this.estoqueVazios = estoqueVazios;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public static class Builder {
        private Long id;
        private String sku;
        private String nome;
        private BigDecimal capacidadeLitros;
        private boolean retornavel = true;
        private TipoProduto tipoProduto;
        private BigDecimal precoVenda;
        private int estoqueCheios = 0;
        private int estoqueVazios = 0;
        private boolean ativo = true;
        private Long versao;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder sku(String sku) {
            this.sku = sku;
            return this;
        }

        public Builder nome(String nome) {
            this.nome = nome;
            return this;
        }

        public Builder capacidadeLitros(BigDecimal capacidadeLitros) {
            this.capacidadeLitros = capacidadeLitros;
            return this;
        }

        public Builder retornavel(boolean retornavel) {
            this.retornavel = retornavel;
            return this;
        }

        public Builder tipoProduto(TipoProduto tipoProduto) {
            this.tipoProduto = tipoProduto;
            return this;
        }

        public Builder precoVenda(BigDecimal precoVenda) {
            this.precoVenda = precoVenda;
            return this;
        }

        public Builder estoqueCheios(int estoqueCheios) {
            this.estoqueCheios = estoqueCheios;
            return this;
        }

        public Builder estoqueVazios(int estoqueVazios) {
            this.estoqueVazios = estoqueVazios;
            return this;
        }

        public Builder ativo(boolean ativo) {
            this.ativo = ativo;
            return this;
        }

        public Builder versao(Long versao) {
            this.versao = versao;
            return this;
        }

        public Product build() {
            return new Product(id, sku, nome, capacidadeLitros, retornavel, tipoProduto,
                    precoVenda, estoqueCheios, estoqueVazios, ativo, versao);
        }
    }
}
